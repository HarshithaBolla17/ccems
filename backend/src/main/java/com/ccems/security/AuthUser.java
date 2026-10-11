package com.ccems.security;

import com.ccems.model.Role;
import org.springframework.security.core.context.SecurityContextHolder;

/** The authenticated caller, rebuilt from the JWT on every request. */
public record AuthUser(Long userId, String email, Role role) {

    public static AuthUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        return null;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
