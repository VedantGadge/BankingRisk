package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskAnalysisResponse;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import com.example.bankingproject.risk.entity.RiskAlert;
import com.example.bankingproject.risk.enums.RiskLevel;
import com.example.bankingproject.risk.enums.RiskStatus;
import com.example.bankingproject.risk.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisService {

    private final RiskRuleEngine riskRuleEngine;
    private final RiskExplanationService riskExplanationService;
    private final RiskAlertRepository riskAlertRepository;

    /**
     * Asynchronous entry point — used by TransactionService so the transfer API
     * returns immediately without waiting for the LLM call.
     */
    @Async
    public void analyzeAsync(RiskContext context) {
        try {
            analyze(context);
        } catch (Exception e) {
            log.error("[risk-analysis-async] failed transactionId={}",
                    context != null ? context.getTransactionId() : null, e);
        }
    }

    /**
     * Synchronous entry point — used by RiskDemoRunner and tests.
     */
    public RiskAnalysisResponse analyze(RiskContext context) {
        log.info("[risk-analysis] start transactionId={} context={}",
                context != null ? context.getTransactionId() : null, context);

        RiskRuleResultDto ruleResult = riskRuleEngine.evaluate(context);
        log.info("[risk-analysis] rule-engine output transactionId={} result={}",
                context != null ? context.getTransactionId() : null, ruleResult);

        String explanation = buildExplanation(context, ruleResult);
        log.info("[risk-analysis] explanation resolved transactionId={} explanation={}",
                context != null ? context.getTransactionId() : null, explanation);

        RiskAnalysisResponse response = RiskAnalysisResponse.builder()
                .riskScore(ruleResult.getRiskScore())
                .riskLevel(ruleResult.getRiskLevel())
                .triggeredRules(ruleResult.getTriggeredRules())
                .explanation(explanation)
                .build();

        // Persist the risk alert to the database
        persistRiskAlert(context, ruleResult, explanation);

        log.info("[risk-analysis] completed transactionId={} response={}",
                context != null ? context.getTransactionId() : null, response);
        return response;
    }

    private void persistRiskAlert(RiskContext context, RiskRuleResultDto ruleResult, String explanation) {
        try {
            RiskAlert alert = RiskAlert.builder()
                    .transactionId(context.getTransactionId())
                    .userId(context.getIdentitySignals() != null
                            ? context.getIdentitySignals().getFromUserId() : null)
                    .riskLevel(RiskLevel.valueOf(ruleResult.getRiskLevel()))
                    .riskScore(ruleResult.getRiskScore())
                    .summary(explanation.length() > 2000
                            ? explanation.substring(0, 2000) : explanation)
                    .reasonCodes(ruleResult.getTriggeredRules() != null
                            ? String.join("; ", ruleResult.getTriggeredRules()) : null)
                    .riskStatus(RiskStatus.CREATED)
                    .recommendedAction(deriveRecommendedAction(ruleResult.getRiskLevel()))
                    .build();

            riskAlertRepository.save(alert);
            log.info("[risk-analysis] alert persisted transactionId={} alertId={}",
                    context.getTransactionId(), alert.getId());
        } catch (Exception e) {
            log.warn("[risk-analysis] failed to persist risk alert transactionId={}",
                    context.getTransactionId(), e);
        }
    }

    private String deriveRecommendedAction(String riskLevel) {
        return switch (riskLevel.toUpperCase()) {
            case "HIGH" -> "BLOCK_OR_MANUAL_REVIEW";
            case "MEDIUM" -> "REVIEW_BEFORE_APPROVAL";
            default -> "APPROVE_WITH_MONITORING";
        };
    }

    private String buildExplanation(RiskContext context, RiskRuleResultDto ruleResult) {
        if (ruleResult == null || ruleResult.getRiskLevel() == null) {
            return "Risk analysis completed without a risk level.";
        }

        if (isLowRisk(ruleResult.getRiskLevel())) {
            log.info("[risk-analysis] skipping llm explanation transactionId={} riskLevel={}",
                    context != null ? context.getTransactionId() : null, ruleResult.getRiskLevel());
            return "Low risk transaction. No LLM explanation generated.";
        }

        return riskExplanationService.explain(context, ruleResult);
    }

    private boolean isLowRisk(String riskLevel) {
        return "LOW".equalsIgnoreCase(riskLevel);
    }
}