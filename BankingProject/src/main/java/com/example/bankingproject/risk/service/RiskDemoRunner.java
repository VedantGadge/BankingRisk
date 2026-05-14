package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskAnalysisResponse;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("risk-demo")
@RequiredArgsConstructor
@Slf4j
public class RiskDemoRunner implements CommandLineRunner {

    private final RiskAnalysisService riskAnalysisService;
    private final RiskRuleEngine riskRuleEngine;
    private final RiskExplanationService riskExplanationService;

    @Override
    public void run(String... args) {
        RiskContext context = RiskContext.builder()
                .transactionId(999L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(1L)
                        .toUserId(2L)
                        .accountAgeDays(3L)
                        .profileRecentlyChanged(true)
                        .build())
                .behaviorSignals(RiskContext.BehaviorSignals.builder()
                        .recentTransactionCount(6)
                        .totalRecentTransactionAmount(new BigDecimal("25000"))
                        .averageRecentTransactionAmount(new BigDecimal("4166.67"))
                        .failedLoginCount(4)
                        .build())
                .moneyFlowSignals(RiskContext.MoneyFlowSignals.builder()
                        .transactionAmount(new BigDecimal("15000"))
                        .currentBalance(new BigDecimal("17000"))
                        .amountToBalanceRatio(new BigDecimal("0.8823"))
                        .build())
                .build();

        log.info("=== SIMULATED RISK CONTEXT ===\n{}", context);

        RiskRuleResultDto ruleResult = riskRuleEngine.evaluate(context);
        log.info("=== RULE ENGINE OUTPUT ===\n{}", ruleResult);

        String explanation = riskExplanationService.explain(context, ruleResult);
        log.info("=== LLM EXPLANATION / FALLBACK OUTPUT ===\n{}", explanation);

        RiskAnalysisResponse finalResult = riskAnalysisService.analyze(context);
        log.info("=== FINAL ANALYSIS OUTPUT ===\n{}", finalResult);

        System.exit(0);
    }
}