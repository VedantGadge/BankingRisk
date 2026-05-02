package com.example.bankingproject.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull
    private Long toUserId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
}
