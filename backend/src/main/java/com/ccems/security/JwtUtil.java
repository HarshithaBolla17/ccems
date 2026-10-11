package com.ccems.security;

import com.ccems.model.Role;
import com.ccems.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${ccems.jwt.secret}") String secret,
                   @Value("${ccems.jwt.expiration-ms}") long expirationMs,
                   @Value("${spring.profiles.active:}") String profiles) {
        if (profiles.contains("prod") && secret.startsWith("ccems-super-secret")) {
            throw new IllegalStateException(
                    "JWT_SECRET is still the built-in demo value. Set JWT_SECRET to a long random string before running in production.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Signed token that carries the user's role as a claim. */
    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getUserId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Returns the caller, or null if the token is missing, expired or tampered with. */
    public AuthUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long uid = ((Number) c.get("uid")).longValue();
            Role role = Role.valueOf((String) c.get("role"));
            return new AuthUser(uid, c.getSubject(), role);
        } catch (JwtException | IllegalArgumentException | ClassCastException | NullPointerException e) {
            return null;
        }
    }
}
