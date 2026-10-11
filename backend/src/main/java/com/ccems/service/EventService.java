package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EventService {

    private static final Set<String> SORT_FIELDS = Set.of("eventDate", "registrationDeadline", "title", "capacity");

    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final ClubMemberRepository members;
    private final UserRepository users;
    private final ClubService clubService;
    private final CertificateService certificateService;
    private final PaymentService paymentService;
    private final NotificationService notifications;
    private final DtoMapper mapper;

    public EventService(EventRepository events, EventRegistrationRepository registrations,
                        ClubMemberRepository members, UserRepository users, ClubService clubService,
                        CertificateService certificateService, PaymentService paymentService,
                        NotificationService notifications, DtoMapper mapper) {
        this.notifications = notifications;
        this.paymentService = paymentService;
        this.events = events;
        this.registrations = registrations;
        this.members = members;
        this.users = users;
        this.clubService = clubService;
        this.certificateService = certificateService;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public PageResponse<EventDto> feed(AuthUser caller, int page, int size, String q, EventStatus status,
                                       boolean openOnly, String sortBy, String dir) {
        Page<Event> result = events.feed(
                (q == null || q.isBlank()) ? null : q.trim(), status, openOnly, LocalDateTime.now(),
                pageable(page, size, sortBy, dir));
        return toPage(caller, result);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventDto> forClub(AuthUser caller, Long clubId, int page, int size,
                                          String sortBy, String dir) {
        clubService.find(clubId);
        return toPage(caller, events.findByClub_ClubId(clubId, pageable(page, size, sortBy, dir)));
    }

    @Transactional(readOnly = true)
    public EventDto get(AuthUser caller, Long id) {
        return mapper.event(find(id), caller.userId());
    }

    @Transactional(readOnly = true)
    public List<RegistrationDto> registrationsFor(AuthUser caller, Long eventId) {
        Event e = find(eventId);
        clubService.assertCanManage(caller, e.getClub());
        LocalDateTime now = LocalDateTime.now();
        return registrations.findByEvent_EventIdOrderByRegisteredAtAsc(eventId)
                .stream().filter(r -> !DtoMapper.holdExpired(r, now)).map(mapper::registration).toList();
    }

    // ----------------------------------------------------------------- writes

    @Transactional
    public EventDto create(AuthUser caller, Long clubId, EventRequest req) {
        Club club = clubService.find(clubId);
        clubService.assertCanManage(caller, club);
        if (club.getStatus() != ClubStatus.ACTIVE) {
            throw BusinessException.badRequest("This club is inactive, so it can't host new events.");
        }
        validateDates(req);

        Event e = new Event();
        e.setClub(club);
        e.setTitle(req.title().trim());
        e.setDescription(req.description());
        e.setVenue(req.venue());
        e.setEventDate(req.eventDate());
        e.setRegistrationDeadline(req.registrationDeadline());
        e.setCapacity(req.capacity());
        e.setPrice(req.price() == null ? 0 : req.price());
        e.setStatus(EventStatus.UPCOMING);
        events.save(e);
        notifications.notifyClubMembers(club, NotificationType.NEW_EVENT, "New event from " + club.getName(),
                e.getTitle(), "/events?q=" + java.net.URLEncoder.encode(e.getTitle(), java.nio.charset.StandardCharsets.UTF_8),
                caller.userId());
        return mapper.event(e, caller.userId());
    }

    @Transactional
    public EventDto setStatus(AuthUser caller, Long eventId, EventStatus status) {
        Event e = find(eventId);
        clubService.assertCanManage(caller, e.getClub());
        e.setStatus(status);
        return mapper.event(e, caller.userId());
    }

    /**
     * Student registration. Capacity and deadline are enforced here, on the server,
     * under a row lock so two simultaneous requests can't both take the last seat.
     */
    @Transactional
    public EventDto register(AuthUser caller, Long eventId) {
        paymentService.releaseExpiredHolds();
        Event e = events.findByIdForUpdate(eventId)
                .orElseThrow(() -> BusinessException.notFound("Event not found."));

        if (e.getStatus() != EventStatus.UPCOMING) {
            throw BusinessException.badRequest(e.getStatus() == EventStatus.COMPLETED
                    ? "This event has ended." : "Registration is closed for this event.");
        }
        if (e.getClub().getStatus() != ClubStatus.ACTIVE) {
            throw BusinessException.badRequest("This club is not active.");
        }
        if (LocalDateTime.now().isAfter(e.getRegistrationDeadline())) {
            throw BusinessException.badRequest("The registration deadline has passed.");
        }
        if (!members.existsByClub_ClubIdAndStudent_UserId(e.getClub().getClubId(), caller.userId())) {
            throw BusinessException.badRequest("Join " + e.getClub().getName() + " to register for its events.");
        }
        if (registrations.existsByEvent_EventIdAndStudent_UserId(eventId, caller.userId())) {
            throw BusinessException.conflict("You are already registered for this event.");
        }
        if (registrations.countActive(eventId, LocalDateTime.now()) >= e.getCapacity()) {
            throw BusinessException.conflict("This event is full.");
        }

        EventRegistration r = new EventRegistration();
        r.setEvent(e);
        r.setStudent(users.getReferenceById(caller.userId()));
        if (e.getPrice() != null && e.getPrice() > 0) {
            // Paid event: hold the seat while the student pays.
            r.setPaymentStatus(PaymentStatus.PENDING);
            r.setHoldExpiresAt(LocalDateTime.now().plusMinutes(PaymentService.HOLD_MINUTES));
        }
        registrations.save(r);
        if (r.getPaymentStatus() == PaymentStatus.NOT_REQUIRED) {
            notifications.notify(caller.userId(), NotificationType.REGISTRATION, "You're registered",
                    e.getTitle(), "/events/" + eventId + "/hub");
        }
        return mapper.event(e, caller.userId());
    }

    @Transactional
    public EventDto cancelRegistration(AuthUser caller, Long eventId) {
        Event e = events.findByIdForUpdate(eventId)
                .orElseThrow(() -> BusinessException.notFound("Event not found."));
        EventRegistration r = registrations.findByEvent_EventIdAndStudent_UserId(eventId, caller.userId())
                .orElseThrow(() -> BusinessException.badRequest("You are not registered for this event."));
        if (r.getAttendanceStatus() != AttendanceStatus.REGISTERED) {
            throw BusinessException.badRequest("Attendance is already recorded, so this registration can't be cancelled.");
        }
        if (e.getStatus() == EventStatus.COMPLETED || LocalDateTime.now().isAfter(e.getEventDate())) {
            throw BusinessException.badRequest("This event has already started.");
        }
        if (r.getPaymentStatus() == PaymentStatus.PAID) {
            // Refund policy: full refund until the registration deadline.
            if (LocalDateTime.now().isAfter(e.getRegistrationDeadline())) {
                throw BusinessException.badRequest(
                        "Paid registrations can only be cancelled, with a refund, before the registration deadline.");
            }
            paymentService.refundIfPaid(eventId, caller.userId());
        }
        registrations.delete(r);
        registrations.flush();
        return mapper.event(e, caller.userId());
    }

    /** Coordinator marks attendance; every PRESENT mark auto-issues a certificate. */
    @Transactional
    public AttendanceResult markAttendance(AuthUser caller, Long eventId, AttendanceRequest req) {
        Event e = find(eventId);
        clubService.assertCanManage(caller, e.getClub());
        if (e.getStatus() != EventStatus.COMPLETED && LocalDateTime.now().isBefore(e.getEventDate())) {
            throw BusinessException.badRequest("Attendance opens when the event starts.");
        }

        int marked = 0;
        int issued = 0;
        for (AttendanceMark m : req.records()) {
            if (m.status() == AttendanceStatus.REGISTERED) {
                throw BusinessException.badRequest("Mark each student as present or absent.");
            }
            EventRegistration r = registrations.findById(m.registrationId())
                    .orElseThrow(() -> BusinessException.notFound("Registration " + m.registrationId() + " not found."));
            if (!r.getEvent().getEventId().equals(eventId)) {
                throw BusinessException.badRequest("Registration " + m.registrationId() + " belongs to a different event.");
            }
            if (r.getPaymentStatus() == PaymentStatus.PENDING) {
                throw BusinessException.badRequest(r.getStudent().getFullName()
                        + " has not paid yet, so attendance can't be marked.");
            }
            if (r.getAttendanceStatus() == AttendanceStatus.PRESENT && m.status() == AttendanceStatus.ABSENT) {
                throw BusinessException.conflict(r.getStudent().getFullName()
                        + " was marked present and already has a certificate. It can't be changed.");
            }
            if (r.getAttendanceStatus() != m.status()) {
                r.setAttendanceStatus(m.status());
                marked++;
            }
            if (m.status() == AttendanceStatus.PRESENT && certificateService.issueIfMissing(r)) {
                issued++;
            }
        }
        if (e.getStatus() != EventStatus.COMPLETED) {
            e.setStatus(EventStatus.COMPLETED);
        }
        List<RegistrationDto> all = registrations.findByEvent_EventIdOrderByRegisteredAtAsc(eventId)
                .stream().map(mapper::registration).toList();
        return new AttendanceResult(marked, issued, all);
    }

    @Transactional
    public EventDto rate(AuthUser caller, Long eventId, int rating) {
        Event e = find(eventId);
        EventRegistration r = registrations.findByEvent_EventIdAndStudent_UserId(eventId, caller.userId())
                .orElseThrow(() -> BusinessException.badRequest("You didn't register for this event."));
        if (r.getAttendanceStatus() != AttendanceStatus.PRESENT) {
            throw BusinessException.badRequest("Only students who attended can leave feedback.");
        }
        r.setFeedbackRating(rating);
        return mapper.event(e, caller.userId());
    }

    // ---------------------------------------------------------------- helpers

    public Event find(Long id) {
        return events.findById(id).orElseThrow(() -> BusinessException.notFound("Event not found."));
    }

    private void validateDates(EventRequest req) {
        LocalDateTime now = LocalDateTime.now();
        if (!req.eventDate().isAfter(now)) {
            throw BusinessException.badRequest("The event date must be in the future.");
        }
        if (!req.registrationDeadline().isAfter(now)) {
            throw BusinessException.badRequest("The registration deadline must be in the future.");
        }
        if (req.registrationDeadline().isAfter(req.eventDate())) {
            throw BusinessException.badRequest("Registration must close before the event starts.");
        }
    }

    private org.springframework.data.domain.Pageable pageable(int page, int size, String sortBy, String dir) {
        String field = SORT_FIELDS.contains(sortBy) ? sortBy : "eventDate";
        Sort sort = "desc".equalsIgnoreCase(dir) ? Sort.by(field).descending() : Sort.by(field).ascending();
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort);
    }

    private PageResponse<EventDto> toPage(AuthUser caller, Page<Event> result) {
        return new PageResponse<>(
                result.getContent().stream().map(e -> mapper.event(e, caller.userId())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
