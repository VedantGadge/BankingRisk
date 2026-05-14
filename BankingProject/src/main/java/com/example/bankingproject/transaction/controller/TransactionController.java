package com.example.bankingproject.transaction.controller;

import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.dto.TransferRequest;
import com.example.bankingproject.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction and transfer APIs")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Transfer money between users")
    @PostMapping("/transfer")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request){
        Long fromUserId = Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );
        return transactionService.transfer(fromUserId,request.getToUserId(),request.getAmount());
    }

    @Operation(summary = "Get paginated transaction history")
    @GetMapping("/history")
    public Page<TransactionResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue="10") int size
    ){

        Long userId = Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );

        size = Math.min(size, 50);
        return transactionService.getHistory(userId, page, size);

    }

}
