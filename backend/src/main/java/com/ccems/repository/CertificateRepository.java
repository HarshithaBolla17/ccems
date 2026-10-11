package com.ccems.repository;

import com.ccems.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByRegistration_RegistrationId(Long registrationId);

    List<Certificate> findByRegistration_Student_UserIdOrderByIssuedAtDesc(Long studentId);
}
