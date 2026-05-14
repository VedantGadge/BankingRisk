package com.example.bankingproject.account.controller;

import com.example.bankingproject.account.dto.AmountRequest;
import com.example.bankingproject.account.dto.AccountResponse;
import com.example.bankingproject.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account", description = "Account management APIs")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Get current user's account")
    @GetMapping
    public AccountResponse getAccount(){
        Long userId = getCurrentUserId();
        log.debug("Get account request from userId: {}", userId);
        return accountService.getAccount(userId);
    }

    @Operation(summary = "Withdraw money from account")
    @PostMapping("/withdraw")
    public AccountResponse withdraw(@Valid @RequestBody AmountRequest request){
        Long userId = getCurrentUserId();
        log.debug("Withdraw request from userId: {}, amount: {}", userId, request.getAmount());
        return accountService.withdraw(userId, request.getAmount());
    }

    @Operation(summary = "Deposit money into account")
    @PostMapping("/deposit")
    public AccountResponse deposit(@Valid @RequestBody AmountRequest request){
        Long userId = getCurrentUserId();
        log.debug("Deposit request from userId: {}, amount: {}", userId, request.getAmount());
        return accountService.deposit(userId, request.getAmount());
    }

    /**
     * Extracts userId from JWT token in SecurityContext
     * @return userId as Long
     */
    private Long getCurrentUserId(){
        return Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );
    }

}
