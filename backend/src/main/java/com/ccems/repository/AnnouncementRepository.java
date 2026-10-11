package com.ccems.repository;

import com.ccems.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** Pinned first, then newest first. */
    List<Announcement> findByEvent_EventIdOrderByPinnedDescCreatedAtDesc(Long eventId);

    long countByEvent_EventId(Long eventId);
}
