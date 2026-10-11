package com.ccems.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Join table for Event <-> Student (many-to-many with extra columns). */
@Entity
@Table(name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    private AttendanceStatus attendanceStatus = AttendanceStatus.REGISTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, columnDefinition = "varchar(20) default 'NOT_REQUIRED'")
    private PaymentStatus paymentStatus = PaymentStatus.NOT_REQUIRED;

    /** For paid events: the seat is held until this moment while the student pays. */
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    /** Optional, 1 to 5. */
    @Column(name = "feedback_rating")
    private Integer feedbackRating;
}
