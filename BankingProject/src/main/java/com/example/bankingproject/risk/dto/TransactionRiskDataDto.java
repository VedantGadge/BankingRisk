package com.example.bankingproject.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRiskDataDto {
    private Integer recentTransactionCount;
    private BigDecimal totalRecentTransactionAmount;
    private BigDecimal averageRecentTransactionAmount;
}
