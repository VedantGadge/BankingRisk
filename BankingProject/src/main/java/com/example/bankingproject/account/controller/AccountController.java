package com.example.bankingproject.account.controller;

import com.example.bankingproject.account.dto.AmountRequest;
import com.example.bankingproject.account.entity.Account;
import com.example.bankingproject.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public Account getAccount(){
        Long userId = Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );

        return accountService.getAccount(userId);
    }

    @PostMapping("/withdraw")
    public Account withdraw(@Valid @RequestBody AmountRequest request){ // using the AmountRequest DTO
        long userId = Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );


        return accountService.withdraw(userId, request.getAmount());
    }

    @PostMapping("/deposit")
    public Account deposit(@Valid @RequestBody AmountRequest request){
        long userId = Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );

        return accountService.deposit(userId, request.getAmount());
    }

}
