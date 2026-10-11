package com.ccems.repository;

import com.ccems.model.EventResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventResourceRepository extends JpaRepository<EventResource, Long> {

    List<EventResource> findByEvent_EventIdOrderByCreatedAtDesc(Long eventId);
}
