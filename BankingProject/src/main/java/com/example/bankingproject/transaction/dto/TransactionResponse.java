package com.example.bankingproject.transaction.dto;

import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    @NotNull
    private Long id;
    private Long fromUserId;
    private Long toUserId;

    @NotNull(message="Amount cannot be null.")
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private LocalDateTime createdAt;
    @NotNull
    private TransactionType type;
    @NotNull
    private TransactionStatus status;
}
