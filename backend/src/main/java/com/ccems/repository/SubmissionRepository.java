package com.ccems.repository;

import com.ccems.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByTask_TaskIdAndStudent_UserId(Long taskId, Long studentId);

    List<Submission> findByTask_TaskIdOrderBySubmittedAtAsc(Long taskId);

    long countByTask_TaskId(Long taskId);
}
