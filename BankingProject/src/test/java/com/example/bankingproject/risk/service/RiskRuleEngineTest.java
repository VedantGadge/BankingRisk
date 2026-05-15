package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the RiskRuleEngine's scoring logic directly — no mocks needed
 * because it is a pure function (input context → output score + rules).
 *
 * These tests document the exact fraud detection thresholds,
 * making it easy to catch regressions when rules are tuned.
 */
class RiskRuleEngineTest {

    private final RiskRuleEngine engine = new RiskRuleEngine();

    // ─────────────────────────────────────────────────
    // Null / empty input
    // ─────────────────────────────────────────────────

    @Test
    void evaluate_shouldReturnLowRiskForNullContext() {
        RiskRuleResultDto result = engine.evaluate(null);

        assertEquals("LOW", result.getRiskLevel());
        assertEquals(0, result.getRiskScore());
    }

    @Test
    void evaluate_shouldReturnLowRiskForEmptyContext() {
        RiskContext context = RiskContext.builder().transactionId(1L).build();

        RiskRuleResultDto result = engine.evaluate(context);

        assertEquals("LOW", result.getRiskLevel());
        assertEquals(0, result.getRiskScore());
        assertTrue(result.getTriggeredRules().isEmpty());
    }

    // ─────────────────────────────────────────────────
    // Identity signal rules
    // ─────────────────────────────────────────────────

    @Test
    void evaluate_shouldFlagNewAccountUnder30Days() {
        RiskContext context = RiskContext.builder()
                .transactionId(1L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(1L)
                        .toUserId(2L)
                        .accountAgeDays(15L)      // Under 30 → triggers rule
                        .profileRecentlyChanged(false)
                        .build())
                .build();

        RiskRuleResultDto result = engine.evaluate(context);

        assertTrue(result.getRiskScore() > 0);
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(r -> r.contains("younger than 30 days")));
    }

    @Test
    void evaluate_shouldReturnHighRiskForSelfTransfer() {
        RiskContext context = RiskContext.builder()
                .transactionId(1L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(1L)
                        .toUserId(1L)     // Same user — should score 100 immediately
                        .accountAgeDays(100L)
                        .profileRecentlyChanged(false)
                        .build())
                .build();

        RiskRuleResultDto result = engine.evaluate(context);

        assertEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(r -> r.contains("same user")));
    }

    // ─────────────────────────────────────────────────
    // Money flow signal rules
    // ─────────────────────────────────────────────────

    @Test
    void evaluate_shouldFlagHighAmountToBalanceRatio() {
        RiskContext context = RiskContext.builder()
                .transactionId(1L)
                .moneyFlowSignals(RiskContext.MoneyFlowSignals.builder()
                        .transactionAmount(new BigDecimal("9500"))
                        .currentBalance(new BigDecimal("10000"))
                        .amountToBalanceRatio(new BigDecimal("0.95"))  // 95% → triggers rule
                        .build())
                .build();

        RiskRuleResultDto result = engine.evaluate(context);

        assertTrue(result.getRiskScore() >= 30);
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(r -> r.contains("high relative to balance")));
    }

    // ─────────────────────────────────────────────────
    // Cross-signal combo rules (ATO / Bust-out)
    // ─────────────────────────────────────────────────

    @Test
    void evaluate_shouldFlagAtoPatternProfileChangeAfterFailedLogins() {
        RiskContext context = RiskContext.builder()
                .transactionId(1L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(1L)
                        .toUserId(2L)
                        .accountAgeDays(60L)
                        .profileRecentlyChanged(true)   // Profile changed...
                        .build())
                .behaviorSignals(RiskContext.BehaviorSignals.builder()
                        .failedLoginCount(3)             // ...after failed logins → ATO
                        .recentTransactionCount(2)
                        .totalRecentTransactionAmount(new BigDecimal("1000"))
                        .averageRecentTransactionAmount(new BigDecimal("500"))
                        .build())
                .build();

        RiskRuleResultDto result = engine.evaluate(context);

        // ATO rule adds 30 points alone — should be at least MEDIUM
        assertTrue(result.getRiskScore() >= 35);
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(r -> r.contains("ATO Suspected")));
    }

    @Test
    void evaluate_shouldFlagBustOutPatternBrandNewAccountWithHighVolume() {
        RiskContext context = RiskContext.builder()
                .transactionId(1L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(1L)
                        .toUserId(2L)
                        .accountAgeDays(3L)               // Brand new account...
                        .profileRecentlyChanged(false)
                        .build())
                .behaviorSignals(RiskContext.BehaviorSignals.builder()
                        .recentTransactionCount(6)         // ...with 6 transactions = bust-out
                        .failedLoginCount(0)
                        .totalRecentTransactionAmount(new BigDecimal("5000"))
                        .averageRecentTransactionAmount(new BigDecimal("833"))
                        .build())
                .build();

        RiskRuleResultDto result = engine.evaluate(context);

        assertEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(r -> r.contains("Bust-out Suspected")));
    }
}
