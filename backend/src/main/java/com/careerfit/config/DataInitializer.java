package com.careerfit.config;

import com.careerfit.entity.Role;
import com.careerfit.entity.User;
import com.careerfit.repository.RoleRepository;
import com.careerfit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates an admin account on startup when ADMIN_EMAIL and ADMIN_PASSWORD are set. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AppProperties props;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        String email = props.admin().email();
        String password = props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters; admin account was not created.");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            return;
        }
        User admin = new User();
        admin.setEmail(normalized);
        admin.setFullName("Administrator");
        admin.setPasswordHash(encoder.encode(password));
        for (String name : new String[]{"USER", "ADMIN"}) {
            Role r = roles.findByName(name).orElseThrow();
            admin.getRoles().add(r);
        }
        users.save(admin);
        log.info("Admin account created for {}", normalized);
    }
}
