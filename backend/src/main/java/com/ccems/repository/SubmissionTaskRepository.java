package com.ccems.repository;

import com.ccems.model.SubmissionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SubmissionTaskRepository extends JpaRepository<SubmissionTask, Long> {

    List<SubmissionTask> findByEvent_EventIdOrderByDeadlineAsc(Long eventId);

    @Query("""
            select t from SubmissionTask t
            where t.event.eventId in :eventIds and t.deadline > :now
            order by t.deadline asc
            """)
    List<SubmissionTask> findOpenForEvents(@Param("eventIds") Collection<Long> eventIds,
                                           @Param("now") LocalDateTime now);
}
