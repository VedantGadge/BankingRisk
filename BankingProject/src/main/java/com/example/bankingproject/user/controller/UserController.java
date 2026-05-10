package com.example.bankingproject.user.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController // Tells Spring: this class handles HTTP requests and returns JSON responses
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @GetMapping("/api/user/me")
    public Map<String, Object> getCurrentUser(){ //returns user data as JSON

        // Extract userId from SecurityContext
        String userId = (String) SecurityContextHolder
                .getContext()
                .getAuthentication() // Retrieves the Authentication object (set by JWT filter)
                .getPrincipal(); // Gets the actual user identity

        return Map.of("userId", userId,"message","Authenticated.");
    }
}

/*
Request → JWT Filter → sets userId in SecurityContext
                     ↓
Controller reads it using SecurityContextHolder
                     ↓
Returns userId as response
 */
