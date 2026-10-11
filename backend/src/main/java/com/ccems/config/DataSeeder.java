package com.ccems.config;

import com.ccems.model.*;
import com.ccems.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Fills an empty database with realistic demo data so every screen has something to show. */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final CertificateRepository certificates;
    private final PaymentRepository payments;
    private final AnnouncementRepository announcements;
    private final EventResourceRepository resources;
    private final SubmissionTaskRepository tasks;
    private final ChatMessageRepository chatMessages;
    private final NotificationRepository notifications;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DataSeeder(UserRepository users, StudentProfileRepository profiles, ClubRepository clubs,
                      ClubMemberRepository members, EventRepository events,
                      EventRegistrationRepository registrations, CertificateRepository certificates,
                      PaymentRepository payments, AnnouncementRepository announcements,
                      EventResourceRepository resources, SubmissionTaskRepository tasks,
                      ChatMessageRepository chatMessages, NotificationRepository notifications,
                      PasswordEncoder encoder, @Value("${ccems.seed.enabled:true}") boolean enabled) {
        this.payments = payments;
        this.announcements = announcements;
        this.resources = resources;
        this.tasks = tasks;
        this.chatMessages = chatMessages;
        this.notifications = notifications;
        this.users = users;
        this.profiles = profiles;
        this.clubs = clubs;
        this.members = members;
        this.events = events;
        this.registrations = registrations;
        this.certificates = certificates;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || users.count() > 0) return;

        // ---- people
        user("Dr. Meera Iyer", "admin@ccems.edu", "Admin@123", Role.ADMIN);
        User arjun = user("Arjun Menon", "coordinator@ccems.edu", "Coord@123", Role.COORDINATOR);
        User sana = user("Sana Qureshi", "coordinator2@ccems.edu", "Coord@123", Role.COORDINATOR);
        User ravi = user("Ravi Teja", "coordinator3@ccems.edu", "Coord@123", Role.COORDINATOR);

        User harshitha = student("B. Harshitha", "student@ccems.edu", "24EG105P55", "CSE-P", 2);
        User nikhil = student("Nikhil Rao", "nikhil@ccems.edu", "24EG105P12", "CSE-P", 2);
        User ananya = student("Ananya Reddy", "ananya@ccems.edu", "24EG105Q07", "ECE", 2);
        User karthik = student("Karthik Varma", "karthik@ccems.edu", "23EG105M33", "ME", 3);
        User fatima = student("Fatima Sheikh", "fatima@ccems.edu", "23EG105A21", "CSE-A", 3);
        User rohan = student("Rohan Das", "rohan@ccems.edu", "25EG105P44", "CSE-P", 1);
        User isha = student("Isha Kulkarni", "isha@ccems.edu", "24EG105D09", "EEE", 2);

        // ---- clubs
        Club coding = club("Codestorm", "Technology",
                "Weekly problem-solving sessions, hackathons and open-source sprints for people who like building things that run.", arjun);
        Club robotics = club("Robotics Guild", "Technology",
                "Design, solder and program autonomous bots. We compete at inter-college events every semester.", arjun);
        Club drama = club("Rangmanch Drama Society", "Arts",
                "Street plays, stage productions and improv nights. No acting experience needed, only nerve.", sana);
        Club music = club("Sargam Music Club", "Arts",
                "A home for singers, instrumentalists and producers. Open mics every month and one big night each term.", sana);
        Club photo = club("Lens & Light", "Arts",
                "Photo walks, editing workshops and an annual campus exhibition.", ravi);
        Club debate = club("Parliament Debating Society", "Literary",
                "Parliamentary-style debates, MUNs and public speaking practice.", ravi);

        // ---- memberships
        join(coding, harshitha, nikhil, ananya, fatima, rohan, karthik);
        join(robotics, harshitha, karthik, isha, ananya);
        join(drama, harshitha, fatima, isha);
        join(music, ananya, rohan);
        join(photo, nikhil, isha);
        join(debate, fatima, rohan, karthik);

        // ---- events
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // Completed event with attendance + certificates already issued
        Event past1 = event(coding, "DSA Sprint: Graphs & Trees",
                "A four-hour guided problem-solving sprint. Bring a laptop; we bring the problems.",
                "Lab 3, CS Block", now.minusDays(9).withHour(14), now.minusDays(11), 40, EventStatus.COMPLETED);
        Event past2 = event(drama, "Nukkad Natak Workshop",
                "Learn to hold a crowd with no stage, no mic and no script.",
                "Open Air Theatre", now.minusDays(4).withHour(16), now.minusDays(6), 30, EventStatus.COMPLETED);

        price(past2, 100);
        attend(past1, harshitha, AttendanceStatus.PRESENT, 5);
        attend(past1, nikhil, AttendanceStatus.PRESENT, 4);
        attend(past1, ananya, AttendanceStatus.ABSENT, null);
        attend(past1, fatima, AttendanceStatus.PRESENT, 5);
        attend(past2, harshitha, AttendanceStatus.PRESENT, 5);
        attend(past2, fatima, AttendanceStatus.PRESENT, 4);
        attend(past2, isha, AttendanceStatus.ABSENT, null);

        // Open for registration
        Event hack = event(coding, "Hack the Campus 2026",
                "A 24-hour hackathon. Build something the campus actually needs, from a late-bus tracker to a mess-menu bot. Teams of up to four.",
                "Innovation Hub", now.plusDays(12).withHour(9), now.plusDays(8), 80, EventStatus.UPCOMING);
        Event line = event(robotics, "Line Follower Challenge",
                "Build a bot that follows the track fastest. Kits provided for first-time teams.",
                "Mechatronics Lab", now.plusDays(6).withHour(10), now.plusDays(4), 24, EventStatus.UPCOMING);
        Event improv = event(drama, "Improv Night",
                "Games, scenes and a lot of laughter. Come as an audience or sign up to play.",
                "Auditorium Green Room", now.plusDays(3).withHour(18), now.plusDays(2), 40, EventStatus.UPCOMING);
        Event openMic = event(music, "Open Mic Evening",
                "Five minutes on stage. Original or cover, solo or with a band.",
                "Student Plaza", now.plusDays(5).withHour(17), now.plusDays(4), 50, EventStatus.UPCOMING);
        Event walk = event(photo, "Golden Hour Photo Walk",
                "A guided walk across campus at sunset, followed by a short editing session.",
                "Main Gate", now.plusDays(7).withHour(16), now.plusDays(6), 30, EventStatus.UPCOMING);
        Event mun = event(debate, "Inter-Department MUN",
                "Two committees, four hours of diplomacy. Position papers due at registration.",
                "Seminar Hall B", now.plusDays(15).withHour(10), now.plusDays(10), 60, EventStatus.UPCOMING);

        // Full event (capacity reached) and one whose deadline has just passed
        Event full = event(robotics, "Drone Basics Bootcamp",
                "Hands-on flight, tuning and safety. Small batch so everyone gets time on the sticks.",
                "Mechatronics Lab", now.plusDays(2).withHour(11), now.plusDays(1), 3, EventStatus.UPCOMING);
        Event late = event(coding, "Git & GitHub in an Afternoon",
                "Branching, pull requests and not breaking main.",
                "Lab 1, CS Block", now.plusDays(1).withHour(15), now.minusHours(3), 40, EventStatus.UPCOMING);

        price(hack, 150);
        price(full, 300);
        price(mun, 200);

        register(full, nikhil);
        register(full, karthik);
        register(full, isha);
        register(late, nikhil);
        register(hack, nikhil);
        register(hack, ananya);
        register(hack, fatima);
        register(line, karthik);
        register(line, isha);
        register(improv, fatima);
        register(improv, isha);
        register(line, harshitha);
        register(hack, harshitha);

        // ---- event hub: announcements, links, a hand-in task
        announce(hack, arjun, "Team formation is on Friday",
                "Teams can have up to four people. Use the event chat to find teammates.", true, false);
        announce(hack, arjun, "Venue update: Innovation Hub, 2nd floor",
                "The ground-floor lab is being repaired. Same building, one floor up.", false, true);
        announce(improv, sana, "Wear something comfortable",
                "We will be on our feet for most of the evening.", false, false);
        link(hack, arjun, "Rules and problem statements", "https://example.com/hack-the-campus/rules",
                "Read this before the day. This is a sample link for the demo.");
        link(hack, arjun, "Starter repository", "https://github.com/",
                "Fork a starter project to save the first hour.");
        task(hack, arjun, "Submit your project repository link",
                "Paste a public link to your repository, with a short README that explains what you built.",
                hack.getEventDate().plusHours(6));

        // ---- chats
        LocalDateTime t = LocalDateTime.now();
        chat(ChatScope.EVENT, hack.getEventId(), arjun, "Welcome, everyone. Ask questions here and I will answer.", t.minusHours(5));
        chat(ChatScope.EVENT, hack.getEventId(), nikhil, "Is a team of five allowed?", t.minusHours(4));
        chat(ChatScope.EVENT, hack.getEventId(), arjun, "Four at most. Teams of five can't be judged fairly.", t.minusHours(4).plusMinutes(6));
        chat(ChatScope.EVENT, hack.getEventId(), ananya, "Looking for a frontend teammate. I can do the backend.", t.minusHours(2));
        chat(ChatScope.CLUB, coding.getClubId(), arjun, "Weekly sprint is back on Thursday at 5 PM in Lab 3.", t.minusDays(1));
        chat(ChatScope.CLUB, coding.getClubId(), fatima, "Can we do a session on graph problems?", t.minusHours(20));

        // ---- bell
        note(harshitha, NotificationType.ANNOUNCEMENT, "Hack the Campus 2026", "Venue update: Innovation Hub, 2nd floor", "/events/" + hack.getEventId() + "/hub", t.minusHours(3));
        note(harshitha, NotificationType.TASK, "New task: Submit your project repository link", "Hack the Campus 2026", "/events/" + hack.getEventId() + "/hub?tab=tasks", t.minusHours(2));
        note(harshitha, NotificationType.NEW_EVENT, "New event from Sargam Music Club", "Open Mic Evening", "/events?q=Open+Mic+Evening", t.minusDays(1));
    }

    private void announce(Event event, User author, String title, String body, boolean pinned, boolean urgent) {
        Announcement a = new Announcement();
        a.setEvent(event);
        a.setAuthor(author);
        a.setTitle(title);
        a.setBody(body);
        a.setPinned(pinned);
        a.setUrgent(urgent);
        announcements.save(a);
    }

    private void link(Event event, User by, String title, String url, String description) {
        EventResource r = new EventResource();
        r.setEvent(event);
        r.setUploadedBy(by);
        r.setTitle(title);
        r.setDescription(description);
        r.setResourceType(ResourceType.LINK);
        r.setLinkUrl(url);
        resources.save(r);
    }

    private void task(Event event, User by, String title, String description, LocalDateTime deadline) {
        SubmissionTask t = new SubmissionTask();
        t.setEvent(event);
        t.setCreatedBy(by);
        t.setTitle(title);
        t.setDescription(description);
        t.setDeadline(deadline);
        tasks.save(t);
    }

    private void chat(ChatScope scope, Long id, User sender, String body, LocalDateTime at) {
        ChatMessage m = new ChatMessage();
        m.setScope(scope);
        m.setScopeId(id);
        m.setSender(sender);
        m.setBody(body);
        m.setCreatedAt(at);
        chatMessages.save(m);
    }

    private void note(User user, NotificationType type, String title, String body, String link, LocalDateTime at) {
        Notification n = new Notification();
        n.setUserId(user.getUserId());
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLinkUrl(link);
        n.setCreatedAt(at);
        notifications.save(n);
    }

    // ------------------------------------------------------------------ helpers

    private User user(String name, String email, String password, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        return users.save(u);
    }

    private User student(String name, String email, String roll, String branch, int year) {
        User u = user(name, email, "Student@123", Role.STUDENT);
        StudentProfile p = new StudentProfile();
        p.setUser(u);
        p.setRollNo(roll);
        p.setBranch(branch);
        p.setYear(year);
        profiles.save(p);
        return u;
    }

    private Club club(String name, String category, String description, User coordinator) {
        Club c = new Club();
        c.setName(name);
        c.setCategory(category);
        c.setDescription(description);
        c.setCoordinator(coordinator);
        c.setStatus(ClubStatus.ACTIVE);
        return clubs.save(c);
    }

    private void join(Club club, User... students) {
        for (User s : students) {
            ClubMember m = new ClubMember();
            m.setClub(club);
            m.setStudent(s);
            members.save(m);
        }
    }

    private Event event(Club club, String title, String description, String venue,
                        LocalDateTime date, LocalDateTime deadline, int capacity, EventStatus status) {
        Event e = new Event();
        e.setClub(club);
        e.setTitle(title);
        e.setDescription(description);
        e.setVenue(venue);
        e.setEventDate(date);
        e.setRegistrationDeadline(deadline);
        e.setCapacity(capacity);
        e.setStatus(status);
        return events.save(e);
    }

    private void price(Event event, int rupees) {
        event.setPrice(rupees);
        events.save(event);
    }

    private EventRegistration register(Event event, User student) {
        EventRegistration r = new EventRegistration();
        r.setEvent(event);
        r.setStudent(student);
        if (event.getPrice() != null && event.getPrice() > 0) {
            // Seed data: paid events come with a completed payment.
            r.setPaymentStatus(PaymentStatus.PAID);
            Payment p = new Payment();
            p.setEvent(event);
            p.setStudent(student);
            p.setAmount(event.getPrice());
            p.setMethod("UPI");
            p.setReference("SEED-" + event.getEventId() + "-" + student.getUserId());
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(LocalDateTime.now().minusDays(1));
            payments.save(p);
        }
        return registrations.save(r);
    }

    private void attend(Event event, User student, AttendanceStatus status, Integer rating) {
        EventRegistration r = register(event, student);
        r.setAttendanceStatus(status);
        r.setFeedbackRating(rating);
        if (status == AttendanceStatus.PRESENT) {
            Certificate c = new Certificate();
            c.setRegistration(r);
            certificates.save(c);
            c.setCertificateUrl("/certificate/" + c.getCertificateId());
        }
    }
}
