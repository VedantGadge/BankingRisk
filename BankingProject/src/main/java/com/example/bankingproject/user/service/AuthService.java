package com.example.bankingproject.user.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.common.security.JwtService;
import com.example.bankingproject.user.dto.LoginRequest;
import com.example.bankingproject.user.dto.RegisterRequest;
import com.example.bankingproject.user.entity.User;
import com.example.bankingproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bankingproject.user.entity.LoginAudit;
import com.example.bankingproject.user.enums.LoginStatus;
import com.example.bankingproject.user.repository.LoginAuditRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final LoginAuditRepository loginAuditRepository;

    @Transactional
    public void register(RegisterRequest request){

        //Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())){
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new RuntimeException("User already exists!");
        }

        //Create user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        //Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        // Create associated account for the new user
        accountService.createAccount(savedUser.getId());
        log.info("Account created for newly registered user: {}", savedUser.getId());
    }

    public String login(LoginRequest request){

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login attempt with non-existent email: {}", request.getEmail());
                    return new IllegalStateException("User does not exist!");
                });

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            log.warn("Failed login attempt for user: {}", request.getEmail());

            loginAuditRepository.save(LoginAudit.builder()
                    .userId(user.getId())
                    .status(LoginStatus.FAILED)
                    .failureReason("INVALID_PASSWORD")
                    .build());

            throw new RuntimeException("Invalid credentials");
        }

        loginAuditRepository.save(LoginAudit.builder()
                .userId(user.getId())
                .status(LoginStatus.SUCCESS)
                .build());

        log.info("User logged in successfully: {}", user.getId());
        return jwtService.generateToken(user.getId().toString());
    }
}
