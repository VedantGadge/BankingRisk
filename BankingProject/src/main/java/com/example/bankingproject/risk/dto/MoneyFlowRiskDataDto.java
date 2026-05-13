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
public class MoneyFlowRiskDataDto {
    private BigDecimal transactionAmount;
    private BigDecimal currentBalance;
    private BigDecimal amountToBalanceRatio;
}
