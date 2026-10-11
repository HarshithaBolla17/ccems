package com.ccems;

import com.ccems.dto.Dtos.EventDto;
import com.ccems.dto.Dtos.EventRequest;
import com.ccems.dto.Dtos.VerifyDto;
import com.ccems.dto.HubDtos.AnnouncementRequest;
import com.ccems.dto.HubDtos.HomeDto;
import com.ccems.dto.HubDtos.HubDto;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import com.ccems.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the rules the whole product depends on, against the demo data and an in-memory database.
 * Run with: mvn test
 */
@SpringBootTest
@ActiveProfiles({"h2", "test"})
class CcemsRulesTest {

    @Autowired UserRepository users;
    @Autowired ClubRepository clubs;
    @Autowired EventRepository events;
    @Autowired EventRegistrationRepository registrations;
    @Autowired CertificateRepository certificates;
    @Autowired EventService eventService;
    @Autowired ClubService clubService;
    @Autowired PaymentService paymentService;
    @Autowired HubService hubService;
    @Autowired ChatService chatService;
    @Autowired NotificationService notificationService;
    @Autowired CertificateService certificateService;
    @Autowired FileStorageService storage;
    @Autowired HomeService homeService;

    // ------------------------------------------------------------------ helpers

    private AuthUser as(String email) {
        User u = users.findByEmailIgnoreCase(email).orElseThrow();
        return new AuthUser(u.getUserId(), u.getEmail(), u.getRole());
    }

    private Long eventId(String title) {
        return events.findAll().stream().filter(e -> e.getTitle().equals(title)).findFirst().orElseThrow().getEventId();
    }

    private Long clubId(String name) {
        return clubs.findAll().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow().getClubId();
    }

    private AuthUser newStudent() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User u = new User();
        u.setFullName("Test " + tag);
        u.setEmail(tag + "@test.edu");
        u.setPasswordHash("x");
        u.setRole(Role.STUDENT);
        users.save(u);
        return new AuthUser(u.getUserId(), u.getEmail(), u.getRole());
    }

    private EventDto newEvent(int capacity, int price) {
        EventRequest req = new EventRequest("Test event " + UUID.randomUUID().toString().substring(0, 6),
                "d", "v", LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(3), capacity, price);
        return eventService.create(as("coordinator@ccems.edu"), clubId("Codestorm"), req);
    }

    private static void assertRefused(HttpStatus expected, String messagePart, Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(expected, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains(messagePart.toLowerCase()),
                "Expected message containing '" + messagePart + "' but was: " + ex.getMessage());
    }

    // ----------------------------------------------------------- registration rules

    @Test
    void fullEventRefusesNewRegistrations() {
        // "Drone Basics Bootcamp" is seeded with capacity 3 and 3 registrations.
        assertRefused(HttpStatus.CONFLICT, "full",
                () -> eventService.register(as("student@ccems.edu"), eventId("Drone Basics Bootcamp")));
    }

    @Test
    void passedDeadlineRefusesRegistration() {
        assertRefused(HttpStatus.BAD_REQUEST, "deadline",
                () -> eventService.register(as("ananya@ccems.edu"), eventId("Git & GitHub in an Afternoon")));
    }

    @Test
    void studentMustJoinTheClubFirst() {
        assertRefused(HttpStatus.BAD_REQUEST, "join",
                () -> eventService.register(as("isha@ccems.edu"), eventId("Hack the Campus 2026")));
    }

    @Test
    void paidEventHoldsASeatThenConfirmsOnPayment() {
        EventDto ev = newEvent(10, 200);
        AuthUser student = newStudent();
        clubService.join(student, ev.clubId());

        EventDto held = eventService.register(student, ev.eventId());
        assertEquals(PaymentStatus.PENDING, held.myPayment());
        assertNotNull(held.holdExpiresAt());

        EventDto paid = paymentService.pay(student, ev.eventId(), "UPI");
        assertEquals(PaymentStatus.PAID, paid.myPayment());
        assertEquals(200, paymentService.receipt(student, ev.eventId()).amount());

        assertRefused(HttpStatus.CONFLICT, "already paid",
                () -> paymentService.pay(student, ev.eventId(), "UPI"));

        // Cancelling before the deadline refunds the payment.
        eventService.cancelRegistration(student, ev.eventId());
        assertEquals(PaymentStatus.REFUNDED, paymentService.receipt(student, ev.eventId()).status());
    }

    @Test
    void lastSeatGoesToExactlyOneStudentUnderConcurrency() throws Exception {
        final int capacity = 5;
        final int students = 20;
        EventDto ev = newEvent(capacity, 0);

        List<AuthUser> pool = new ArrayList<>();
        for (int i = 0; i < students; i++) {
            AuthUser s = newStudent();
            clubService.join(s, ev.clubId());
            pool.add(s);
        }

        ExecutorService executor = Executors.newFixedThreadPool(students);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (AuthUser s : pool) {
            futures.add(executor.submit(() -> {
                go.await();
                try {
                    eventService.register(s, ev.eventId());
                    succeeded.incrementAndGet();
                } catch (BusinessException full) {
                    // expected for everyone after the first five
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(capacity, succeeded.get());
        assertEquals(capacity, registrations.countByEvent_EventId(ev.eventId()));
    }

    // ------------------------------------------------------------------ event hub

    @Test
    void onlyRegisteredStudentsCanOpenTheHub() {
        Long hack = eventId("Hack the Campus 2026");
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(BusinessException.class, () -> hubService.hub(as("isha@ccems.edu"), hack)).getStatus());

        HubDto hub = hubService.hub(as("student@ccems.edu"), hack);
        assertFalse(hub.canManage());
        assertTrue(hub.announcements().size() >= 2);
        assertTrue(hub.announcements().get(0).pinned(), "pinned announcements come first");
        assertTrue(hub.resources().size() >= 2);
        assertFalse(hub.tasks().isEmpty());

        assertTrue(hubService.hub(as("coordinator@ccems.edu"), hack).canManage());
    }

    @Test
    void studentsCannotPostAnnouncements() {
        Long hack = eventId("Hack the Campus 2026");
        assertRefused(HttpStatus.FORBIDDEN, "coordinator",
                () -> hubService.postAnnouncement(as("student@ccems.edu"), hack,
                        new AnnouncementRequest("Free pizza", "Come now", false, false)));
    }

    @Test
    void anAnnouncementNotifiesRegisteredStudents() {
        Long hack = eventId("Hack the Campus 2026");
        AuthUser student = as("nikhil@ccems.edu");
        long before = notificationService.unread(student.userId());

        hubService.postAnnouncement(as("coordinator@ccems.edu"), hack,
                new AnnouncementRequest("Doors open at 8:30", "Bring your ID card.", false, false));

        assertEquals(before + 1, notificationService.unread(student.userId()));
    }

    @Test
    void uploadsAcceptDocumentsAndRejectProgramsAndHugeFiles() {
        FileStorageService.Stored stored = storage.store(
                new MockMultipartFile("file", "notes.pdf", "application/pdf", "hello".getBytes()));
        assertTrue(stored.storedName().endsWith(".pdf"));
        assertEquals("notes.pdf", stored.originalName());
        assertNotNull(storage.load(stored.storedName()));
        storage.delete(stored.storedName());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () ->
                storage.store(new MockMultipartFile("file", "run.exe", "application/octet-stream", new byte[]{1}))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () ->
                storage.store(new MockMultipartFile("file", "page.html", "text/html", "<b>x</b>".getBytes()))).getStatus());
        byte[] tooBig = new byte[(int) FileStorageService.MAX_BYTES + 1];
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, assertThrows(BusinessException.class, () ->
                storage.store(new MockMultipartFile("file", "big.pdf", "application/pdf", tooBig))).getStatus());
    }

    // ----------------------------------------------------------------------- chat

    @Test
    void aMutedStudentCannotSendUntilUnmuted() {
        Long hack = eventId("Hack the Campus 2026");
        AuthUser coordinator = as("coordinator@ccems.edu");
        AuthUser ananya = as("ananya@ccems.edu");

        chatService.mute(coordinator, "event", hack, ananya.userId());
        assertRefused(HttpStatus.FORBIDDEN, "muted", () -> chatService.send(ananya, "event", hack, "hello"));

        chatService.unmute(coordinator, "event", hack, ananya.userId());
        assertEquals("hello", chatService.send(ananya, "event", hack, "hello").body());
    }

    @Test
    void outsidersCannotReadAChat() {
        Long hack = eventId("Hack the Campus 2026");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(BusinessException.class,
                () -> chatService.messages(as("isha@ccems.edu"), "event", hack)).getStatus());
        assertFalse(chatService.messages(as("student@ccems.edu"), "event", hack).messages().isEmpty());
    }

    @Test
    void aStudentCannotMuteOrDeleteOthersMessages() {
        Long hack = eventId("Hack the Campus 2026");
        AuthUser nikhil = as("nikhil@ccems.edu");
        Long someoneElsesMessage = chatService.messages(nikhil, "event", hack).messages().stream()
                .filter(m -> !m.mine()).findFirst().orElseThrow().messageId();

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(BusinessException.class,
                () -> chatService.delete(nikhil, someoneElsesMessage)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(BusinessException.class,
                () -> chatService.mute(nikhil, "event", hack, as("ananya@ccems.edu").userId())).getStatus());
    }

    // -------------------------------------------------------- certificates and home

    @Test
    void certificateCodesVerifyPublicly() {
        Certificate c = certificates.findAll().get(0);
        VerifyDto ok = certificateService.verify(DtoMapper.code(c));
        assertTrue(ok.valid());
        assertNotNull(ok.studentName());

        assertFalse(certificateService.verify("CCEMS-2000-00001").valid());
        assertFalse(certificateService.verify("not-a-code").valid());
        assertFalse(certificateService.verify("").valid());
    }

    @Test
    void everyRoleGetsAHomePage() {
        HomeDto student = homeService.home(as("student@ccems.edu"));
        assertEquals("STUDENT", student.role());
        assertNotNull(student.nextEvent());
        assertEquals(3, student.stats().size());
        assertNotNull(student.latestCertificate());

        HomeDto coordinator = homeService.home(as("coordinator@ccems.edu"));
        assertEquals("COORDINATOR", coordinator.role());
        assertFalse(coordinator.managedClubs().isEmpty());

        HomeDto admin = homeService.home(as("admin@ccems.edu"));
        assertEquals("ADMIN", admin.role());
        assertEquals(4, admin.stats().size());
    }
}
