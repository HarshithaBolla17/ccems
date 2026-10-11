package com.ccems.repository;

import com.ccems.model.AttendanceStatus;
import com.ccems.model.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByEvent_EventIdAndStudent_UserId(Long eventId, Long studentId);

    boolean existsByEvent_EventIdAndStudent_UserId(Long eventId, Long studentId);

    long countByEvent_EventId(Long eventId);

    /** Seats actually taken: confirmed registrations plus unpaid holds that have not expired yet. */
    @Query("""
            select count(r) from EventRegistration r
            where r.event.eventId = :eventId
              and (r.paymentStatus <> com.ccems.model.PaymentStatus.PENDING or r.holdExpiresAt > :now)
            """)
    long countActive(@Param("eventId") Long eventId, @Param("now") java.time.LocalDateTime now);

    @Query("""
            select r from EventRegistration r
            where r.paymentStatus = com.ccems.model.PaymentStatus.PENDING and r.holdExpiresAt <= :now
            """)
    List<EventRegistration> findExpiredHolds(@Param("now") java.time.LocalDateTime now);

    long countByEvent_EventIdAndAttendanceStatus(Long eventId, AttendanceStatus status);

    long countByEvent_EventIdAndPaymentStatus(Long eventId, com.ccems.model.PaymentStatus status);

    List<EventRegistration> findByEvent_EventIdOrderByRegisteredAtAsc(Long eventId);

    List<EventRegistration> findByStudent_UserIdOrderByRegisteredAtDesc(Long studentId);

    @Query("select r.event.eventId from EventRegistration r where r.student.userId = :studentId")
    List<Long> findEventIdsByStudent(@Param("studentId") Long studentId);

    @Query("select r.event.eventId, count(r) from EventRegistration r group by r.event.eventId")
    List<Object[]> countRegistrationsPerEvent();
}
