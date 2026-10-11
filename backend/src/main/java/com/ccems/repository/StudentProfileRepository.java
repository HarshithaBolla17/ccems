package com.ccems.repository;

import com.ccems.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser_UserId(Long userId);
    boolean existsByRollNoIgnoreCase(String rollNo);
}
