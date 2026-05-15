package com.example.bankingproject.user.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.common.security.JwtService;
import com.example.bankingproject.user.dto.LoginRequest;
import com.example.bankingproject.user.dto.RegisterRequest;
import com.example.bankingproject.user.entity.User;
import com.example.bankingproject.user.repository.LoginAuditRepository;
import com.example.bankingproject.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AccountService accountService;
    @Mock private LoginAuditRepository loginAuditRepository;

    @InjectMocks
    private AuthService authService;

    // ─────────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────────

    @Test
    void register_shouldSaveUserAndCreateAccount() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(accountService, times(1)).createAccount(any());
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("taken@gmail.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail("taken@gmail.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(request));

        // Verify user is never saved when email already exists
        verify(userRepository, never()).save(any(User.class));
        verify(accountService, never()).createAccount(any());
    }

    @Test
    void register_shouldEncodePassword() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@gmail.com");
        request.setPassword("plaintext");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("bcrypt-hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // Verify the password is encoded, never stored in plain text
            assertEquals("bcrypt-hashed", u.getPassword());
            return u;
        });

        authService.register(request);

        verify(passwordEncoder, times(1)).encode("plaintext");
    }

    // ─────────────────────────────────────────────────
    // login
    // ─────────────────────────────────────────────────

    @Test
    void login_shouldReturnJwtToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        User user = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .password("hashed")
                .role("USER")
                .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(jwtService.generateToken("1")).thenReturn("jwt-token");

        String token = authService.login(request);

        assertEquals("jwt-token", token);
        verify(userRepository, times(1)).findByEmail("test@gmail.com");
        verify(passwordEncoder, times(1)).matches("123456", "hashed");
        verify(loginAuditRepository, times(1)).save(any());
        verify(jwtService, times(1)).generateToken("1");
    }

    @Test
    void login_shouldThrowWhenUserDoesNotExist() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@gmail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("ghost@gmail.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> authService.login(request));

        // JWT should never be generated for non-existent user
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_shouldThrowAndAuditFailedLoginOnWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("wrongpassword");

        User user = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .password("hashed")
                .role("USER")
                .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request));

        // Failed login must be audited — this feeds the risk engine
        verify(loginAuditRepository, times(1)).save(any());
        // JWT must never be issued on bad password
        verify(jwtService, never()).generateToken(any());
    }
}