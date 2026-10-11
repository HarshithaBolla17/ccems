package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.model.*;
import com.ccems.repository.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/** Turns entities into response DTOs. Call inside a transaction (lazy associations). */
@Component
public class DtoMapper {

    private final StudentProfileRepository profiles;
    private final ClubMemberRepository members;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final CertificateRepository certificates;

    public DtoMapper(StudentProfileRepository profiles, ClubMemberRepository members,
                     EventRepository events, EventRegistrationRepository registrations,
                     CertificateRepository certificates) {
        this.profiles = profiles;
        this.members = members;
        this.events = events;
        this.registrations = registrations;
        this.certificates = certificates;
    }

    public UserDto user(User u) {
        Optional<StudentProfile> p = profiles.findByUser_UserId(u.getUserId());
        return new UserDto(u.getUserId(), u.getFullName(), u.getEmail(), u.getRole(),
                p.map(StudentProfile::getBranch).orElse(null),
                p.map(StudentProfile::getYear).orElse(null),
                p.map(StudentProfile::getRollNo).orElse(null));
    }

    public ClubDto club(Club c, Long viewerId) {
        User coord = c.getCoordinator();
        boolean joined = viewerId != null && members.existsByClub_ClubIdAndStudent_UserId(c.getClubId(), viewerId);
        return new ClubDto(c.getClubId(), c.getName(), c.getDescription(), c.getCategory(), c.getStatus(),
                coord == null ? null : coord.getUserId(),
                coord == null ? null : coord.getFullName(),
                members.countByClub_ClubId(c.getClubId()),
                events.countByClub_ClubIdAndStatus(c.getClubId(), EventStatus.UPCOMING),
                joined);
    }

    public EventDto event(Event e, Long viewerId) {
        LocalDateTime now = LocalDateTime.now();
        long count = registrations.countActive(e.getEventId(), now);
        long seatsLeft = Math.max(0, e.getCapacity() - count);

        String reason = null;
        if (e.getStatus() == EventStatus.COMPLETED) reason = "This event has ended.";
        else if (e.getStatus() == EventStatus.CLOSED) reason = "Registration was closed by the coordinator.";
        else if (e.getClub().getStatus() != ClubStatus.ACTIVE) reason = "This club is not active.";
        else if (now.isAfter(e.getRegistrationDeadline())) reason = "The registration deadline has passed.";
        else if (seatsLeft == 0) reason = "This event is full.";

        boolean registered = false;
        boolean memberOfClub = false;
        AttendanceStatus att = null;
        Integer rating = null;
        PaymentStatus myPayment = null;
        LocalDateTime hold = null;
        if (viewerId != null) {
            memberOfClub = members.existsByClub_ClubIdAndStudent_UserId(e.getClub().getClubId(), viewerId);
            Optional<EventRegistration> reg =
                    registrations.findByEvent_EventIdAndStudent_UserId(e.getEventId(), viewerId);
            // An unpaid hold that ran out no longer counts as a registration.
            if (reg.isPresent() && !holdExpired(reg.get(), now)) {
                registered = true;
                att = reg.get().getAttendanceStatus();
                rating = reg.get().getFeedbackRating();
                myPayment = reg.get().getPaymentStatus();
                hold = reg.get().getHoldExpiresAt();
            }
        }

        int price = e.getPrice() == null ? 0 : e.getPrice();
        return new EventDto(e.getEventId(), e.getClub().getClubId(), e.getClub().getName(),
                e.getClub().getCategory(), e.getTitle(), e.getDescription(), e.getVenue(),
                e.getEventDate(), e.getRegistrationDeadline(), e.getCapacity(), count, seatsLeft,
                e.getStatus(), reason == null, reason, registered, att, rating, memberOfClub,
                price, myPayment, hold);
    }

    public RegistrationDto registration(EventRegistration r) {
        User s = r.getStudent();
        Event e = r.getEvent();
        Optional<StudentProfile> p = profiles.findByUser_UserId(s.getUserId());
        Long certId = certificates.findByRegistration_RegistrationId(r.getRegistrationId())
                .map(Certificate::getCertificateId).orElse(null);
        int price = e.getPrice() == null ? 0 : e.getPrice();
        return new RegistrationDto(r.getRegistrationId(), e.getEventId(), e.getTitle(), e.getClub().getName(),
                e.getEventDate(), s.getUserId(), s.getFullName(), s.getEmail(),
                p.map(StudentProfile::getRollNo).orElse(null),
                p.map(StudentProfile::getBranch).orElse(null),
                p.map(StudentProfile::getYear).orElse(null),
                r.getRegisteredAt(), r.getAttendanceStatus(), r.getFeedbackRating(), certId,
                r.getPaymentStatus(), r.getPaymentStatus() == PaymentStatus.PAID ? price : 0);
    }

    public CertificateDto certificate(Certificate c) {
        EventRegistration r = c.getRegistration();
        User s = r.getStudent();
        Event e = r.getEvent();
        Optional<StudentProfile> p = profiles.findByUser_UserId(s.getUserId());
        return new CertificateDto(c.getCertificateId(), code(c), s.getUserId(), s.getFullName(),
                p.map(StudentProfile::getRollNo).orElse(null),
                p.map(StudentProfile::getBranch).orElse(null),
                e.getTitle(), e.getClub().getName(), e.getEventDate(), c.getIssuedAt(),
                c.getCertificateUrl());
    }

    /** Public, human-friendly verification code, e.g. CCEMS-2026-00012. */
    public static String code(Certificate c) {
        return String.format("CCEMS-%d-%05d", c.getIssuedAt().getYear(), c.getCertificateId());
    }

    public static boolean holdExpired(EventRegistration r, LocalDateTime now) {
        return r.getPaymentStatus() == PaymentStatus.PENDING
                && r.getHoldExpiresAt() != null && !r.getHoldExpiresAt().isAfter(now);
    }
}
