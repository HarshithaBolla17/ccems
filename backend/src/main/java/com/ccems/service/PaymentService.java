package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Paid events. The gateway here is a stand-in: pay() records a successful payment straight away.
 * To use a real gateway (e.g. Razorpay), create the order before pay(), verify the gateway's
 * signature inside pay(), and store its payment id in Payment.reference.
 */
@Service
public class PaymentService {

    public static final int HOLD_MINUTES = 10;
    private static final Set<String> METHODS = Set.of("UPI", "CARD", "NETBANKING");

    private final PaymentRepository payments;
    private final EventRegistrationRepository registrations;
    private final EventRepository events;
    private final StudentProfileRepository profiles;
    private final NotificationService notifications;
    private final DtoMapper mapper;

    public PaymentService(PaymentRepository payments, EventRegistrationRepository registrations,
                          EventRepository events, StudentProfileRepository profiles,
                          NotificationService notifications, DtoMapper mapper) {
        this.notifications = notifications;
        this.payments = payments;
        this.registrations = registrations;
        this.events = events;
        this.profiles = profiles;
        this.mapper = mapper;
    }

    /** Frees seats whose payment window ran out. Runs every minute and before each registration. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        List<EventRegistration> expired = registrations.findExpiredHolds(LocalDateTime.now());
        if (!expired.isEmpty()) registrations.deleteAll(expired);
    }

    @Transactional
    public EventDto pay(AuthUser caller, Long eventId, String method) {
        String m = method == null ? "" : method.trim().toUpperCase();
        if (!METHODS.contains(m)) throw BusinessException.badRequest("Choose UPI, card or net banking.");

        Event e = events.findById(eventId).orElseThrow(() -> BusinessException.notFound("Event not found."));
        EventRegistration r = registrations.findByEvent_EventIdAndStudent_UserId(eventId, caller.userId())
                .orElseThrow(() -> BusinessException.badRequest("Register for this event before paying."));

        if (r.getPaymentStatus() == PaymentStatus.PAID) {
            throw BusinessException.conflict("You have already paid for this event.");
        }
        if (r.getPaymentStatus() != PaymentStatus.PENDING) {
            throw BusinessException.badRequest("This event is free, so there is nothing to pay.");
        }
        if (DtoMapper.holdExpired(r, LocalDateTime.now())) {
            throw BusinessException.badRequest("Your seat hold expired. Register again to get a new one.");
        }

        Payment p = new Payment();
        p.setEvent(e);
        p.setStudent(r.getStudent());
        p.setAmount(e.getPrice());
        p.setMethod(m);
        p.setReference("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        payments.save(p);

        r.setPaymentStatus(PaymentStatus.PAID);
        r.setHoldExpiresAt(null);
        notifications.notify(caller.userId(), NotificationType.PAYMENT, "Payment received",
                "₹" + e.getPrice() + " for " + e.getTitle(), "/events/" + eventId + "/hub");
        return mapper.event(e, caller.userId());
    }

    /** Refunds the student's paid payment for this event, if there is one. */
    @Transactional
    public void refundIfPaid(Long eventId, Long studentId) {
        payments.findFirstByEvent_EventIdAndStudent_UserIdAndStatus(eventId, studentId, PaymentStatus.PAID)
                .ifPresent(p -> {
                    p.setStatus(PaymentStatus.REFUNDED);
                    p.setRefundedAt(LocalDateTime.now());
                });
    }

    @Transactional(readOnly = true)
    public ReceiptDto receipt(AuthUser caller, Long eventId) {
        Payment p = payments.findFirstByEvent_EventIdAndStudent_UserIdOrderByCreatedAtDesc(eventId, caller.userId())
                .orElseThrow(() -> BusinessException.notFound("No payment found for this event."));
        StudentProfile profile = profiles.findByUser_UserId(caller.userId()).orElse(null);
        return new ReceiptDto(
                String.format("RCPT-%d-%05d", p.getCreatedAt().getYear(), p.getPaymentId()),
                p.getReference(), p.getStudent().getFullName(),
                profile == null ? null : profile.getRollNo(),
                p.getEvent().getTitle(), p.getEvent().getClub().getName(), p.getEvent().getEventDate(),
                p.getAmount(), p.getMethod(), p.getStatus(), p.getPaidAt(), p.getRefundedAt());
    }
}
