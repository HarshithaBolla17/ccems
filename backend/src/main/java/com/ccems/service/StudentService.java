package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.User;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final UserRepository users;
    private final ClubMemberRepository members;
    private final EventRegistrationRepository registrations;
    private final CertificateRepository certificates;
    private final DtoMapper mapper;

    public StudentService(UserRepository users, ClubMemberRepository members,
                          EventRegistrationRepository registrations,
                          CertificateRepository certificates, DtoMapper mapper) {
        this.users = users;
        this.members = members;
        this.registrations = registrations;
        this.certificates = certificates;
        this.mapper = mapper;
    }

    /** Joined clubs, event history and earned certificates for one student. */
    @Transactional(readOnly = true)
    public ProfileDto profile(AuthUser caller, Long studentId) {
        if (caller.role() == com.ccems.model.Role.STUDENT && !caller.userId().equals(studentId)) {
            throw BusinessException.forbidden("You can only view your own profile.");
        }
        User u = users.findById(studentId)
                .orElseThrow(() -> BusinessException.notFound("Student not found."));
        return new ProfileDto(
                mapper.user(u),
                members.findByStudent_UserIdOrderByJoinedAtDesc(studentId).stream()
                        .map(m -> mapper.club(m.getClub(), studentId)).toList(),
                registrations.findByStudent_UserIdOrderByRegisteredAtDesc(studentId).stream()
                        .filter(r -> !DtoMapper.holdExpired(r, java.time.LocalDateTime.now()))
                        .map(mapper::registration).toList(),
                certificates.findByRegistration_Student_UserIdOrderByIssuedAtDesc(studentId).stream()
                        .map(mapper::certificate).toList());
    }
}
