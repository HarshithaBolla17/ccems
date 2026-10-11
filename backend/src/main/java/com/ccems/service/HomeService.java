package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.dto.HubDtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/** Builds the "what should I do today?" page for each role. */
@Service
public class HomeService {

    private final UserRepository users;
    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final CertificateRepository certificates;
    private final SubmissionTaskRepository tasks;
    private final SubmissionRepository submissions;
    private final AnnouncementRepository announcements;
    private final PaymentRepository payments;
    private final NotificationService notifications;
    private final ChatService chat;
    private final DtoMapper mapper;

    public HomeService(UserRepository users, ClubRepository clubs, ClubMemberRepository members,
                       EventRepository events, EventRegistrationRepository registrations,
                       CertificateRepository certificates, SubmissionTaskRepository tasks,
                       SubmissionRepository submissions, AnnouncementRepository announcements,
                       PaymentRepository payments, NotificationService notifications,
                       ChatService chat, DtoMapper mapper) {
        this.users = users;
        this.clubs = clubs;
        this.members = members;
        this.events = events;
        this.registrations = registrations;
        this.certificates = certificates;
        this.tasks = tasks;
        this.submissions = submissions;
        this.announcements = announcements;
        this.payments = payments;
        this.notifications = notifications;
        this.chat = chat;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public HomeDto home(AuthUser u) {
        User user = users.findById(u.userId()).orElseThrow(() -> BusinessException.notFound("Account not found."));
        return switch (u.role()) {
            case STUDENT -> student(u, user);
            case COORDINATOR -> staff(u, user, false);
            case ADMIN -> staff(u, user, true);
        };
    }

    // ---------------------------------------------------------------- student

    private HomeDto student(AuthUser u, User user) {
        LocalDateTime now = LocalDateTime.now();
        Long uid = u.userId();
        List<AttentionItem> attention = new ArrayList<>();

        List<EventRegistration> regs = registrations.findByStudent_UserIdOrderByRegisteredAtDesc(uid).stream()
                .filter(r -> !DtoMapper.holdExpired(r, now)).toList();

        Event next = null;
        long attended = 0;
        List<Long> openEventIds = new ArrayList<>();
        for (EventRegistration r : regs) {
            Event e = r.getEvent();
            if (r.getPaymentStatus() == PaymentStatus.PENDING) {
                attention.add(new AttentionItem("payment", "Pay for " + e.getTitle(),
                        "Your seat is held for a few more minutes.", "/events?q=" + enc(e.getTitle()),
                        "urgent", r.getHoldExpiresAt()));
                continue;
            }
            openEventIds.add(e.getEventId());
            if (r.getAttendanceStatus() == AttendanceStatus.PRESENT) {
                attended++;
                if (r.getFeedbackRating() == null) {
                    attention.add(new AttentionItem("rate", "Rate " + e.getTitle(),
                            "Tell the club how it went.", "/profile", "info", null));
                }
            }
            if (e.getStatus() != EventStatus.COMPLETED && e.getEventDate().isAfter(now)
                    && (next == null || e.getEventDate().isBefore(next.getEventDate()))) {
                next = e;
            }
        }

        if (!openEventIds.isEmpty()) {
            for (SubmissionTask t : tasks.findOpenForEvents(openEventIds, now)) {
                boolean done = submissions.findByTask_TaskIdAndStudent_UserId(t.getTaskId(), uid).isPresent();
                if (!done && t.getDeadline().isBefore(now.plusDays(3))) {
                    attention.add(new AttentionItem("task", "Hand in: " + t.getTitle(),
                            t.getEvent().getTitle(), "/events/" + t.getEvent().getEventId() + "/hub?tab=tasks",
                            "urgent", t.getDeadline()));
                }
            }
        }

        long unreadChats = chat.unreadTotal(u);
        if (unreadChats > 0) {
            attention.add(new AttentionItem("chat", unreadChats + " unread chat message" + (unreadChats > 1 ? "s" : ""),
                    "Open your chats to catch up.", "/chats", "info", null));
        }

        // Open events the student has not registered for, from joined clubs first.
        Set<Long> joined = new HashSet<>(members.findClubIdsByStudent(uid));
        Set<Long> registeredIds = new HashSet<>();
        regs.forEach(r -> registeredIds.add(r.getEvent().getEventId()));
        List<Event> open = new ArrayList<>(events.findOpenUpcoming(now, PageRequest.of(0, 30, Sort.by("eventDate"))));
        open.removeIf(e -> registeredIds.contains(e.getEventId()));
        open.sort(Comparator.comparing((Event e) -> !joined.contains(e.getClub().getClubId()))
                .thenComparing(Event::getEventDate));
        List<EventDto> suggestedEvents = open.stream().map(e -> mapper.event(e, uid))
                .filter(EventDto::registrationOpen).limit(4).toList();

        List<ClubDto> suggestedClubs = clubs.search(ClubStatus.ACTIVE, null, PageRequest.of(0, 50, Sort.by("name")))
                .getContent().stream().filter(c -> !joined.contains(c.getClubId()))
                .map(c -> mapper.club(c, uid))
                .sorted(Comparator.comparingLong(ClubDto::memberCount).reversed())
                .limit(3).toList();

        List<Certificate> certs = certificates.findByRegistration_Student_UserIdOrderByIssuedAtDesc(uid);
        List<Stat> stats = List.of(
                new Stat("Clubs joined", String.valueOf(joined.size()), "active memberships"),
                new Stat("Events attended", String.valueOf(attended), regs.size() + " registered in total"),
                new Stat("Certificates", String.valueOf(certs.size()), "earned so far"));

        return new HomeDto("STUDENT", user.getFullName(),
                next == null ? null : mapper.event(next, uid), attention, suggestedEvents, suggestedClubs,
                List.of(), stats, certs.isEmpty() ? null : mapper.certificate(certs.get(0)),
                unreadChats, notifications.unread(uid));
    }

    // -------------------------------------------------------- coordinator/admin

    private HomeDto staff(AuthUser u, User user, boolean admin) {
        LocalDateTime now = LocalDateTime.now();
        Long uid = u.userId();
        List<Club> mine = admin ? clubs.findAll(Sort.by("name")) : clubs.findByCoordinator_UserId(uid);
        List<AttentionItem> attention = new ArrayList<>();
        List<Event> all = new ArrayList<>();
        for (Club c : mine) {
            all.addAll(events.findByClub_ClubId(c.getClubId(), PageRequest.of(0, 100, Sort.by("eventDate"))).getContent());
        }

        Event next = null;
        long upcoming = 0;
        long seats = 0;
        for (Event e : all) {
            boolean started = !e.getEventDate().isAfter(now);
            if (e.getStatus() != EventStatus.COMPLETED && started) {
                attention.add(new AttentionItem("attendance", "Mark attendance for " + e.getTitle(),
                        "The event has started. Present students get a certificate.",
                        "/coordinator?tab=attendance&event=" + e.getEventId(), "urgent", null));
            } else if (e.getStatus() == EventStatus.COMPLETED && e.getEventDate().isAfter(now.minusDays(14))) {
                long unmarked = registrations.countByEvent_EventIdAndAttendanceStatus(e.getEventId(), AttendanceStatus.REGISTERED);
                if (unmarked > 0) {
                    attention.add(new AttentionItem("attendance", unmarked + " student" + (unmarked > 1 ? "s" : "")
                            + " not marked for " + e.getTitle(), "Finish the attendance sheet.",
                            "/coordinator?tab=attendance&event=" + e.getEventId(), "info", null));
                }
            }
            if (e.getStatus() == EventStatus.UPCOMING && e.getEventDate().isAfter(now)) {
                upcoming++;
                seats += registrations.countActive(e.getEventId(), now);
                if (next == null || e.getEventDate().isBefore(next.getEventDate())) next = e;
                long unpaid = registrations.countByEvent_EventIdAndPaymentStatus(e.getEventId(), PaymentStatus.PENDING);
                if (unpaid > 0) {
                    attention.add(new AttentionItem("payment", unpaid + " student" + (unpaid > 1 ? "s haven't" : " hasn't")
                            + " paid for " + e.getTitle(), "Their seats are held for 10 minutes.",
                            "/coordinator?tab=registrations&event=" + e.getEventId(), "info", null));
                }
                if (e.getEventDate().isBefore(now.plusDays(3)) && announcements.countByEvent_EventId(e.getEventId()) == 0) {
                    attention.add(new AttentionItem("announce", "Post an update for " + e.getTitle(),
                            "It starts soon and has no announcements yet.",
                            "/events/" + e.getEventId() + "/hub", "info", null));
                }
            }
        }

        if (!admin) {
            if (mine.isEmpty()) {
                attention.add(new AttentionItem("setup", "No club is assigned to you yet",
                        "An admin assigns clubs to coordinators.", "/clubs", "info", null));
            }
            for (Club c : mine) {
                boolean hasUpcoming = all.stream().anyMatch(e -> e.getClub().getClubId().equals(c.getClubId())
                        && e.getStatus() == EventStatus.UPCOMING && e.getEventDate().isAfter(now));
                if (!hasUpcoming) {
                    attention.add(new AttentionItem("plan", "Plan the next event for " + c.getName(),
                            "There is nothing upcoming right now.", "/coordinator?new=1", "info", null));
                }
            }
        } else {
            for (Club c : mine) {
                if (c.getStatus() == ClubStatus.ACTIVE && c.getCoordinator() == null) {
                    attention.add(new AttentionItem("setup", c.getName() + " has no coordinator",
                            "Students can join, but nobody can publish events.", "/admin?tab=clubs", "urgent", null));
                }
            }
            for (Event e : all) {
                if (e.getStatus() == EventStatus.UPCOMING && e.getEventDate().isAfter(now)
                        && e.getEventDate().isBefore(now.plusDays(7))) {
                    long count = registrations.countActive(e.getEventId(), now);
                    if (count * 5 < e.getCapacity()) {
                        attention.add(new AttentionItem("low", e.getTitle() + " is only " + (count * 100 / e.getCapacity()) + "% full",
                                "It starts within a week. A reminder from the club may help.", "/admin?tab=events", "info", null));
                    }
                }
            }
        }

        List<Stat> stats = new ArrayList<>();
        if (admin) {
            stats.add(new Stat("Students", String.valueOf(users.findByRole(Role.STUDENT).size()),
                    users.findByRole(Role.COORDINATOR).size() + " coordinators"));
            stats.add(new Stat("Active clubs", String.valueOf(mine.stream().filter(c -> c.getStatus() == ClubStatus.ACTIVE).count()),
                    "of " + mine.size() + " in total"));
            stats.add(new Stat("Upcoming events", String.valueOf(events.countByStatus(EventStatus.UPCOMING)), "across all clubs"));
            stats.add(new Stat("Ticket revenue", "₹" + String.format(Locale.ENGLISH, "%,d", payments.totalCollected()), "after refunds"));
        } else {
            long memberCount = mine.stream().mapToLong(c -> members.countByClub_ClubId(c.getClubId())).sum();
            long revenue = mine.stream().mapToLong(c -> payments.collectedByClub(c.getClubId())).sum();
            stats.add(new Stat("Upcoming events", String.valueOf(upcoming), "in your clubs"));
            stats.add(new Stat("Seats taken", String.valueOf(seats), "across upcoming events"));
            stats.add(new Stat("Members", String.valueOf(memberCount), "in your clubs"));
            stats.add(new Stat("Ticket revenue", "₹" + String.format(Locale.ENGLISH, "%,d", revenue), "after refunds"));
        }

        // Urgent items first.
        attention.sort(Comparator.comparing((AttentionItem a) -> !"urgent".equals(a.tone())));

        List<ClubDto> managed = mine.stream().limit(admin ? 0 : 20).map(c -> mapper.club(c, uid)).toList();
        return new HomeDto(admin ? "ADMIN" : "COORDINATOR", user.getFullName(),
                next == null ? null : mapper.event(next, uid), attention, List.of(), List.of(),
                managed, stats, null, chat.unreadTotal(u), notifications.unread(uid));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
