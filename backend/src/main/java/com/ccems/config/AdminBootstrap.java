package com.ccems.config;

import com.ccems.model.Role;
import com.ccems.model.User;
import com.ccems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * For a real deployment: with demo data off, the database starts empty and nobody could sign in.
 * If ADMIN_EMAIL and ADMIN_PASSWORD are set and there are no users yet, this creates the first admin.
 */
@Component
@Order(2)
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${ccems.bootstrap.admin-email:}") String email,
                          @Value("${ccems.bootstrap.admin-password:}") String password,
                          @Value("${ccems.bootstrap.admin-name:Administrator}") String name) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || password.isBlank() || users.count() > 0) return;
        if (password.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters.");
        }
        User admin = new User();
        admin.setFullName(name);
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        users.save(admin);
        System.out.println("Created the first admin account: " + admin.getEmail());
    }
}
