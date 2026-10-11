package com.ccems.controller;

import com.ccems.dto.Dtos.*;
import com.ccems.security.AuthUser;
import com.ccems.service.CertificateService;
import com.ccems.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentService students;
    private final CertificateService certificates;

    public StudentController(StudentService students, CertificateService certificates) {
        this.students = students;
        this.certificates = certificates;
    }

    @GetMapping("/students/{id}/certificates")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public List<CertificateDto> certificates(@PathVariable Long id) {
        return certificates.forStudent(AuthUser.current(), id);
    }

    @GetMapping("/students/{id}/profile")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ProfileDto profile(@PathVariable Long id) {
        return students.profile(AuthUser.current(), id);
    }

    @GetMapping("/certificates/{id}")
    public CertificateDto certificate(@PathVariable Long id) {
        return certificates.get(AuthUser.current(), id);
    }
}
