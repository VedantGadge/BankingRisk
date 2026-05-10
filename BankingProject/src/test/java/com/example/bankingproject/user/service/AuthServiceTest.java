package com.example.bankingproject.user.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.common.security.JwtService;
import com.example.bankingproject.user.dto.LoginRequest;
import com.example.bankingproject.user.dto.RegisterRequest;
import com.example.bankingproject.user.entity.User;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldSaveUserAndCreateAccount() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(accountService, times(1)).createAccount(any());
    }

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
        verify(jwtService, times(1)).generateToken("1");
    }
}