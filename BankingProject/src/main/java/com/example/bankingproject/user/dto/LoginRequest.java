package com.example.bankingproject.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "Email should be valid.")
    @NotBlank(message = "Email us required.")
    private String email;

    @NotBlank(message = "Email is required.")
    private String password;
}
