package com.example.bankingproject.user.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}
