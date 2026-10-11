package com.ccems.controller;

import com.ccems.dto.Dtos.*;
import com.ccems.security.AuthUser;
import com.ccems.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    /** The signed-in user; used by the frontend to restore a session. */
    @GetMapping("/me")
    public UserDto me() {
        return auth.me(AuthUser.current().userId());
    }
}
