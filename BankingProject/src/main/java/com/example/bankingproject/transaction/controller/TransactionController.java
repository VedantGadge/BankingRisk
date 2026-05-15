package com.example.bankingproject.transaction.controller;

import com.example.bankingproject.common.idempotency.service.IdempotencyService;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.dto.TransferRequest;
import com.example.bankingproject.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/transaction")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction and transfer APIs")
public class TransactionController {

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    @Operation(summary = "Transfer money between users",
            description = "Supports idempotent requests via the Idempotency-Key header. "
                    + "If the same key is sent again, the cached response is returned without reprocessing.")
    @PostMapping("/transfer")
    public TransactionResponse transfer(
            @Parameter(description = "UUID to guarantee exactly-once processing. "
                    + "Retrying with the same key returns the original response.")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        Long fromUserId = getCurrentUserId();

        // No idempotency key — process normally (backward compatible)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return transactionService.transfer(fromUserId, request.getToUserId(), request.getAmount());
        }

        // Check for cached response from a previous identical request
        Optional<TransactionResponse> cached = idempotencyService.getCachedResponse(
                idempotencyKey, TransactionResponse.class);
        if (cached.isPresent()) {
            log.info("[idempotency] returning cached response for key={}", idempotencyKey);
            return cached.get();
        }

        // Claim the key (throws DuplicateRequestException if concurrent)
        idempotencyService.claim(idempotencyKey, "TRANSFER");

        try {
            TransactionResponse response = transactionService.transfer(
                    fromUserId, request.getToUserId(), request.getAmount());
            idempotencyService.complete(idempotencyKey, response);
            return response;
        } catch (Exception e) {
            idempotencyService.fail(idempotencyKey);
            throw e;
        }
    }

    @Operation(summary = "Get paginated transaction history")
    @GetMapping("/history")
    public Page<TransactionResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getCurrentUserId();
        size = Math.min(size, 50);
        return transactionService.getHistory(userId, page, size);
    }

    private Long getCurrentUserId() {
        return Long.parseLong(
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
        );
    }
}
