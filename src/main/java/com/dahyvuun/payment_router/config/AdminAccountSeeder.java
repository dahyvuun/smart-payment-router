package com.dahyvuun.payment_router.config;

import com.dahyvuun.payment_router.domain.Role;
import com.dahyvuun.payment_router.domain.User;
import com.dahyvuun.payment_router.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@payment-router.local";

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        String rawPassword = System.getenv().getOrDefault("ADMIN_INITIAL_PASSWORD", "changeme123!");

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Seeded initial admin account: {}", adminEmail);
    }
}