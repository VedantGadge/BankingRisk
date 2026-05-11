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
    private DeviceSignals deviceSignals;
    private BeneficiarySignals beneficiarySignals;
    private HistoricalSignals historicalSignals;
    private AmlSignals amlSignals;
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
        private Boolean firstTransactionAfterSignup;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorSignals {
        private Integer recentTransactionCount;
        private BigDecimal totalRecentTransactionAmount;
        private BigDecimal averageRecentTransactionAmount;
        private Boolean transactionVelocitySpike;
        private Boolean amountSpike;
        private Boolean failedLoginsBeforeTransfer;
        private Integer failedLoginCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceSignals {
        private Boolean newDevice;
        private Boolean ipChanged;
        private Boolean locationChanged;
        private Boolean impossibleTravel;
        private Boolean suspiciousSession;
        private Integer sessionAgeMinutes;
        private Integer deviceRiskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficiarySignals {
        private Long beneficiaryAgeDays;
        private Boolean newBeneficiary;
        private Boolean beneficiarySeenInPreviousAlerts;
        private Integer numberOfSendersToSameBeneficiary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalSignals {
        private Integer sharedDeviceAccountsCount;
        private Integer sharedRecipientAccountsCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmlSignals {
        private Boolean highAmountToMultipleSmallAccounts;
        private Boolean rapidCashOutSuspected;
        private Integer fanOutCount;
        private Integer fanInCount;
        private BigDecimal totalOutboundAmount24h;
        private BigDecimal totalInboundAmount24h;
        private BigDecimal maxRecipientBalance;
        private Integer numberOfSmallBalanceRecipients;
        private String amlPatternSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoneyFlowSignals {
        private BigDecimal transactionAmount;
        private BigDecimal currentBalance;
        private BigDecimal amountToBalanceRatio;
        private BigDecimal amountVsHistoricalAverageRatio;
        private Integer transfersLastHour;
        private Integer transfersLast24h;
        private Integer uniqueRecipientsLast24h;
        private Boolean cashOutPattern;
    }
}