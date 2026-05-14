package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RiskRuleEngine {

    public RiskRuleResultDto evaluate(RiskContext context) {
        int score = 0;
        List<String> rules = new ArrayList<>();

        if (context == null) {
            log.warn("[risk-rule-engine] received null RiskContext");
            return RiskRuleResultDto.builder()
                    .riskScore(0)
                    .riskLevel("LOW")
                    .triggeredRules(List.of("No risk context provided"))
                    .build();
        }

        log.info("[risk-rule-engine] evaluating transactionId={} context={}", context.getTransactionId(), context);

        // -------------------------
        // Identity rules
        // -------------------------
        if (context.getIdentitySignals() != null) {
            RiskContext.IdentitySignals identity = context.getIdentitySignals();

            if (identity.getAccountAgeDays() != null && identity.getAccountAgeDays() < 30) {
                score += 25;
                rules.add("Account is younger than 30 days");
                log.debug("[risk-rule-engine] matched rule=Account is younger than 30 days transactionId={}", context.getTransactionId());
            }

            if (identity.getAccountAgeDays() != null && identity.getAccountAgeDays() < 7) {
                score += 30;
                rules.add("Account is younger than 7 days");
                log.debug("[risk-rule-engine] matched rule=Account is younger than 7 days transactionId={}", context.getTransactionId());
            }

            if (Boolean.TRUE.equals(identity.getProfileRecentlyChanged())) {
                score += 15;
                rules.add("Profile was changed recently");
                log.debug("[risk-rule-engine] matched rule=Profile was changed recently transactionId={}", context.getTransactionId());
            }

            if (Boolean.TRUE.equals(identity.getProfileRecentlyChanged())
                    && identity.getAccountAgeDays() != null
                    && identity.getAccountAgeDays() < 30) {
                score += 10;
                rules.add("New account with a recent profile change");
                log.debug("[risk-rule-engine] matched rule=New account with a recent profile change transactionId={}", context.getTransactionId());
            }

            if (identity.getFromUserId() != null && identity.getToUserId() != null
                    && identity.getFromUserId().equals(identity.getToUserId())) {
                score += 100;
                rules.add("Sender and receiver are the same user");
                log.debug("[risk-rule-engine] matched rule=Sender and receiver are the same user transactionId={}", context.getTransactionId());
            }
        }

        // -------------------------
        // Behavior rules
        // -------------------------
        if (context.getBehaviorSignals() != null) {
            RiskContext.BehaviorSignals behavior = context.getBehaviorSignals();

            if (behavior.getFailedLoginCount() != null && behavior.getFailedLoginCount() >= 3) {
                score += 20;
                rules.add("Multiple failed login attempts detected");
                log.debug("[risk-rule-engine] matched rule=Multiple failed login attempts detected transactionId={}", context.getTransactionId());
            }

            if (behavior.getFailedLoginCount() != null && behavior.getFailedLoginCount() >= 5) {
                score += 20;
                rules.add("High number of failed login attempts");
                log.debug("[risk-rule-engine] matched rule=High number of failed login attempts transactionId={}", context.getTransactionId());
            }

            if (behavior.getRecentTransactionCount() != null && behavior.getRecentTransactionCount() >= 5) {
                score += 20;
                rules.add("High recent transaction count");
                log.debug("[risk-rule-engine] matched rule=High recent transaction count transactionId={}", context.getTransactionId());
            }

            if (behavior.getTotalRecentTransactionAmount() != null
                    && behavior.getTotalRecentTransactionAmount().compareTo(new BigDecimal("20000")) > 0) {
                score += 20;
                rules.add("High total transaction amount in the last 24 hours");
                log.debug("[risk-rule-engine] matched rule=High total transaction amount in the last 24 hours transactionId={}", context.getTransactionId());
            }

            if (behavior.getAverageRecentTransactionAmount() != null
                    && behavior.getTotalRecentTransactionAmount() != null
                    && behavior.getRecentTransactionCount() != null
                    && behavior.getRecentTransactionCount() > 0) {

                BigDecimal avg = behavior.getAverageRecentTransactionAmount();
                if (avg.compareTo(new BigDecimal("10000")) > 0) {
                    score += 20;
                    rules.add("Average recent transaction amount is very high");
                    log.debug("[risk-rule-engine] matched rule=Average recent transaction amount is very high transactionId={}", context.getTransactionId());
                }

                if (behavior.getRecentTransactionCount() <= 2
                        && avg.compareTo(new BigDecimal("15000")) > 0) {
                    score += 15;
                    rules.add("Low volume but very high average transaction amount");
                    log.debug("[risk-rule-engine] matched rule=Low volume but very high average transaction amount transactionId={}", context.getTransactionId());
                }

                if (behavior.getRecentTransactionCount() >= 5
                        && avg.compareTo(new BigDecimal("5000")) > 0) {
                    score += 15;
                    rules.add("High volume with elevated average transaction amount");
                    log.debug("[risk-rule-engine] matched rule=High volume with elevated average transaction amount transactionId={}", context.getTransactionId());
                }
            }
        }

        // -------------------------
        // Money flow rules
        // -------------------------
        if (context.getMoneyFlowSignals() != null) {
            RiskContext.MoneyFlowSignals flow = context.getMoneyFlowSignals();

            if (flow.getAmountToBalanceRatio() != null
                    && flow.getAmountToBalanceRatio().compareTo(new BigDecimal("0.75")) > 0) {
                score += 30;
                rules.add("Transaction amount is high relative to balance");
                log.debug("[risk-rule-engine] matched rule=Transaction amount is high relative to balance transactionId={}", context.getTransactionId());
            }

            if (flow.getAmountToBalanceRatio() != null
                    && flow.getAmountToBalanceRatio().compareTo(new BigDecimal("0.90")) >= 0) {
                score += 20;
                rules.add("Transaction amount is extremely high relative to balance");
                log.debug("[risk-rule-engine] matched rule=Transaction amount is extremely high relative to balance transactionId={}", context.getTransactionId());
            }

            if (flow.getCurrentBalance() != null
                    && flow.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
                score += 25;
                rules.add("Account balance is zero or negative");
                log.debug("[risk-rule-engine] matched rule=Account balance is zero or negative transactionId={}", context.getTransactionId());
            }

            if (flow.getCurrentBalance() != null
                    && flow.getCurrentBalance().compareTo(new BigDecimal("1000")) < 0
                    && flow.getTransactionAmount() != null
                    && flow.getTransactionAmount().compareTo(new BigDecimal("5000")) > 0) {
                score += 20;
                rules.add("Low balance with a high transaction amount");
                log.debug("[risk-rule-engine] matched rule=Low balance with a high transaction amount transactionId={}", context.getTransactionId());
            }

            if (flow.getTransactionAmount() != null
                    && flow.getTransactionAmount().compareTo(new BigDecimal("50000")) > 0) {
                score += 25;
                rules.add("Transaction amount exceeds high-value threshold");
                log.debug("[risk-rule-engine] matched rule=Transaction amount exceeds high-value threshold transactionId={}", context.getTransactionId());
            }

            if (flow.getTransactionAmount() != null
                    && flow.getTransactionAmount().compareTo(new BigDecimal("75000")) > 0) {
                score += 15;
                rules.add("Transaction amount exceeds very high-value threshold");
                log.debug("[risk-rule-engine] matched rule=Transaction amount exceeds very high-value threshold transactionId={}", context.getTransactionId());
            }
        }

        // Cross-signal combo rules
        if (context.getIdentitySignals() != null && context.getMoneyFlowSignals() != null) {
            RiskContext.IdentitySignals identity = context.getIdentitySignals();
            RiskContext.MoneyFlowSignals flow = context.getMoneyFlowSignals();

            if (identity.getAccountAgeDays() != null
                    && identity.getAccountAgeDays() < 30
                    && flow.getTransactionAmount() != null
                    && flow.getTransactionAmount().compareTo(new BigDecimal("10000")) > 0) {
                score += 20;
                rules.add("New account making a high-value transaction");
                log.debug("[risk-rule-engine] matched rule=New account making a high-value transaction transactionId={}", context.getTransactionId());
            }

            if (Boolean.TRUE.equals(identity.getProfileRecentlyChanged())
                    && flow.getAmountToBalanceRatio() != null
                    && flow.getAmountToBalanceRatio().compareTo(new BigDecimal("0.75")) > 0) {
                score += 15;
                rules.add("Recent profile change with high amount-to-balance ratio");
                log.debug("[risk-rule-engine] matched rule=Recent profile change with high amount-to-balance ratio transactionId={}", context.getTransactionId());
            }

            // Bust-out: Immediate Deposit & Drain
            if (identity.getAccountAgeDays() != null
                    && identity.getAccountAgeDays() < 3
                    && flow.getAmountToBalanceRatio() != null
                    && flow.getAmountToBalanceRatio().compareTo(new BigDecimal("0.95")) >= 0) {
                score += 40;
                rules.add("Bust-out Suspected: Immediate near-complete drain of a very new account");
                log.debug("[risk-rule-engine] matched rule=Bust-out Suspected: Immediate near-complete drain of a very new account transactionId={}", context.getTransactionId());
            }
        }

        if (context.getBehaviorSignals() != null && context.getMoneyFlowSignals() != null) {
            RiskContext.BehaviorSignals behavior = context.getBehaviorSignals();
            RiskContext.MoneyFlowSignals flow = context.getMoneyFlowSignals();

            if (behavior.getFailedLoginCount() != null
                    && behavior.getFailedLoginCount() >= 3
                    && flow.getTransactionAmount() != null
                    && flow.getTransactionAmount().compareTo(new BigDecimal("10000")) > 0) {
                score += 15;
                rules.add("Failed logins followed by a high-value transaction");
                log.debug("[risk-rule-engine] matched rule=Failed logins followed by a high-value transaction transactionId={}", context.getTransactionId());
            }

            // Bust-out: The Siphoning pattern
            if (behavior.getRecentTransactionCount() != null
                    && behavior.getRecentTransactionCount() >= 10
                    && flow.getCurrentBalance() != null
                    && flow.getCurrentBalance().compareTo(new BigDecimal("500")) < 0) {
                score += 20;
                rules.add("Siphoning Suspected: High transaction count driving balance very low");
                log.debug("[risk-rule-engine] matched rule=Siphoning Suspected: High transaction count driving balance very low transactionId={}", context.getTransactionId());
            }

            // Subtle Anomaly: Single massive transaction
            if (behavior.getRecentTransactionCount() != null
                    && behavior.getRecentTransactionCount() == 1
                    && behavior.getTotalRecentTransactionAmount() != null
                    && flow.getTransactionAmount() != null
                    && flow.getTransactionAmount().compareTo(behavior.getTotalRecentTransactionAmount()) == 0
                    && flow.getTransactionAmount().compareTo(new BigDecimal("5000")) > 0) {
                score += 15;
                rules.add("Anomaly: First transaction of the day is unusually large and accounts for 100% of volume");
                log.debug("[risk-rule-engine] matched rule=Anomaly: First transaction of the day is unusually large and accounts for 100% of volume transactionId={}", context.getTransactionId());
            }
        }

        if (context.getIdentitySignals() != null && context.getBehaviorSignals() != null) {
            RiskContext.IdentitySignals identity = context.getIdentitySignals();
            RiskContext.BehaviorSignals behavior = context.getBehaviorSignals();

            // ATO: Struggle, Change, and Drain
            if (Boolean.TRUE.equals(identity.getProfileRecentlyChanged())
                    && behavior.getFailedLoginCount() != null
                    && behavior.getFailedLoginCount() > 0) {
                score += 30;
                rules.add("ATO Suspected: Profile changed after failed logins");
                log.debug("[risk-rule-engine] matched rule=ATO Suspected: Profile changed after failed logins transactionId={}", context.getTransactionId());
            }

            // ATO: Takeover and Spam
            if (Boolean.TRUE.equals(identity.getProfileRecentlyChanged())
                    && behavior.getRecentTransactionCount() != null
                    && behavior.getRecentTransactionCount() >= 5) {
                score += 25;
                rules.add("ATO Suspected: High transaction volume immediately after profile change");
                log.debug("[risk-rule-engine] matched rule=ATO Suspected: High transaction volume immediately after profile change transactionId={}", context.getTransactionId());
            }

            // Bust-out: Brand New Account Frenzy
            if (identity.getAccountAgeDays() != null
                    && identity.getAccountAgeDays() < 7
                    && behavior.getRecentTransactionCount() != null
                    && behavior.getRecentTransactionCount() >= 5) {
                score += 25;
                rules.add("Bust-out Suspected: High transaction volume on a brand new account");
                log.debug("[risk-rule-engine] matched rule=Bust-out Suspected: High transaction volume on a brand new account transactionId={}", context.getTransactionId());
            }
        }

        String riskLevel;
        if (score >= 70) {
            riskLevel = "HIGH";
        } else if (score >= 35) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        RiskRuleResultDto result = RiskRuleResultDto.builder()
                .riskScore(score)
                .riskLevel(riskLevel)
                .triggeredRules(rules)
                .build();

        log.info("[risk-rule-engine] completed transactionId={} score={} level={} triggeredRules={}",
                context.getTransactionId(), score, riskLevel, rules);
        return result;
    }
}