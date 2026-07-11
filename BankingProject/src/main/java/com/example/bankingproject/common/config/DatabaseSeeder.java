package com.example.bankingproject.common.config;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.user.dto.RegisterRequest;
import com.example.bankingproject.user.entity.User;
import com.example.bankingproject.user.repository.UserRepository;
import com.example.bankingproject.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AccountService accountService;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("[seeder] Database is empty. Seeding default demo users...");

            try {
                // Register User 1
                RegisterRequest user1 = new RegisterRequest();
                user1.setEmail("user1@example.com");
                user1.setPassword("password");
                authService.register(user1);

                // Find User 1 to fund their account
                Optional<User> u1 = userRepository.findByEmail("user1@example.com");
                if (u1.isPresent()) {
                    accountService.deposit(u1.get().getId(), new BigDecimal("50000.00"));
                    log.info("[seeder] Seeded user1@example.com (ID: {}) with $50,000 balance", u1.get().getId());
                }

                // Register User 2
                RegisterRequest user2 = new RegisterRequest();
                user2.setEmail("user2@example.com");
                user2.setPassword("password");
                authService.register(user2);

                // Find User 2 to fund their account
                Optional<User> u2 = userRepository.findByEmail("user2@example.com");
                if (u2.isPresent()) {
                    accountService.deposit(u2.get().getId(), new BigDecimal("1000.00"));
                    log.info("[seeder] Seeded user2@example.com (ID: {}) with $1,000 balance", u2.get().getId());
                }

            } catch (Exception e) {
                log.error("[seeder] Failed to seed demo database", e);
            }
        } else {
            log.info("[seeder] Database already contains users. Skipping seeder.");
        }
    }
}
