package com.example.bankingproject.user.service;

import com.example.bankingproject.common.security.JwtService;
import com.example.bankingproject.user.dto.LoginRequest;
import com.example.bankingproject.user.dto.RegisterRequest;
import com.example.bankingproject.user.entity.User;
import com.example.bankingproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor // injects dependencies
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest request){

        //Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())){
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
        userRepository.save(user);

    }

    public String login(LoginRequest request){

        //Check n fetch user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User does not exist!"));

        // Check password
        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            throw new RuntimeException("Invalid credentials");
        }

        //dummy token
        return jwtService.generateToken(user.getId().toString());
    }
}
