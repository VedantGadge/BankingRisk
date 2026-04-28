package com.example.bankingproject.user.controller;


import com.example.bankingproject.user.dto.AuthResponse;
import com.example.bankingproject.user.dto.LoginRequest;
import com.example.bankingproject.user.dto.RegisterRequest;
import com.example.bankingproject.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController //Marks class as a REST API controller and Automatically returns in JSON
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {

        authService.register(request);
        return Map.of("message", "User registered successfully");

    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest login) {

        String token = authService.login(login);
        return new AuthResponse(token);

    }

}

/*
@RequestBody
Converts incoming JSON → Java object

Example request
{
  "email": "test@gmail.com",
  "password": "123456"
}
Becomes
request.getEmail()
request.getPassword()
 */
