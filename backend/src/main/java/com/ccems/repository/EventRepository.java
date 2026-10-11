package com.ccems.repository;

import com.ccems.model.Event;
import com.ccems.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByClub_ClubId(Long clubId, Pageable pageable);

    /** Platform-wide feed with optional text, status and "open registration only" filters. */
    @Query("""
            select e from Event e
            where (:q is null or lower(e.title) like lower(concat('%', :q, '%'))
                   or lower(e.club.name) like lower(concat('%', :q, '%')))
              and (:status is null or e.status = :status)
              and (:openOnly = false or (e.status = com.ccems.model.EventStatus.UPCOMING
                                         and e.registrationDeadline > :now))
            """)
    Page<Event> feed(@Param("q") String q,
                     @Param("status") EventStatus status,
                     @Param("openOnly") boolean openOnly,
                     @Param("now") LocalDateTime now,
                     Pageable pageable);

    /** Pessimistic lock so two simultaneous sign-ups cannot both take the last seat. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.eventId = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(EventStatus status);

    /** Events a student could still register for (before capacity is considered). */
    @Query("""
            select e from Event e
            where e.status = com.ccems.model.EventStatus.UPCOMING
              and e.registrationDeadline > :now
              and e.club.status = com.ccems.model.ClubStatus.ACTIVE
            """)
    java.util.List<Event> findOpenUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    long countByClub_ClubId(Long clubId);

    long countByClub_ClubIdAndStatus(Long clubId, EventStatus status);
}
