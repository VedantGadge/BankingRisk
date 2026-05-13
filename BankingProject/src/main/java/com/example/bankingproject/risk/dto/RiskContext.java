package com.example.bankingproject.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskContext {

    private Long transactionId;

    private IdentitySignals identitySignals;
    private BehaviorSignals behaviorSignals;
    private MoneyFlowSignals moneyFlowSignals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentitySignals {
        private Long fromUserId;
        private Long toUserId;
        private Long accountAgeDays;
        private Boolean profileRecentlyChanged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorSignals {
        private Integer recentTransactionCount;
        private BigDecimal totalRecentTransactionAmount;
        private BigDecimal averageRecentTransactionAmount;
        private Integer failedLoginCount;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoneyFlowSignals {
        private BigDecimal transactionAmount;
        private BigDecimal currentBalance;
        private BigDecimal amountToBalanceRatio;
    }
}