package com.ccems.repository;

import com.ccems.model.Payment;
import com.ccems.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByEvent_EventIdAndStudent_UserIdOrderByCreatedAtDesc(Long eventId, Long studentId);

    Optional<Payment> findFirstByEvent_EventIdAndStudent_UserIdAndStatus(Long eventId, Long studentId, PaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.ccems.model.PaymentStatus.PAID")
    long totalCollected();

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = com.ccems.model.PaymentStatus.PAID and p.event.club.clubId = :clubId
            """)
    long collectedByClub(@Param("clubId") Long clubId);
}
