package com.example.bankingproject.transaction.controller;

import com.example.bankingproject.transaction.dto.TransferRequest;
import com.example.bankingproject.transaction.entity.Transaction;
import com.example.bankingproject.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactin")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public Transaction transfer(@Valid @RequestBody TransferRequest request){
        Long fromUserId = Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );

        return transactionService.transfer(fromUserId,request.getToUserId(),request.getAmount());

    }

}
