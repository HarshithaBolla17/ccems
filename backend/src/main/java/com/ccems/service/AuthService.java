package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.Role;
import com.ccems.model.StudentProfile;
import com.ccems.model.User;
import com.ccems.repository.StudentProfileRepository;
import com.ccems.repository.UserRepository;
import com.ccems.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final DtoMapper mapper;

    public AuthService(UserRepository users, StudentProfileRepository profiles,
                       PasswordEncoder encoder, JwtUtil jwt, DtoMapper mapper) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        Role role = req.role() == null ? Role.STUDENT : req.role();
        if (role == Role.ADMIN) {
            throw BusinessException.forbidden("Admin accounts can't be created through sign-up.");
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw BusinessException.conflict("An account with this email already exists.");
        }
        if (role == Role.STUDENT) {
            if (blank(req.rollNo()) || blank(req.branch()) || req.year() == null) {
                throw BusinessException.badRequest("Students must provide roll number, branch and year.");
            }
            if (profiles.existsByRollNoIgnoreCase(req.rollNo().trim())) {
                throw BusinessException.conflict("This roll number is already registered.");
            }
        }

        User u = new User();
        u.setFullName(req.fullName().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(role);
        users.save(u);

        if (role == Role.STUDENT) {
            StudentProfile p = new StudentProfile();
            p.setUser(u);
            p.setBranch(req.branch().trim());
            p.setYear(req.year());
            p.setRollNo(req.rollNo().trim().toUpperCase());
            profiles.save(p);
        }
        return new AuthResponse(jwt.generate(u), mapper.user(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BadCredentialsException("bad credentials");
        }
        return new AuthResponse(jwt.generate(u), mapper.user(u));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Account not found."));
        return mapper.user(u);
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
