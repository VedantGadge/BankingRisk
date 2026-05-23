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

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisService {

    private final RiskRuleEngine riskRuleEngine;
    private final RiskExplanationService riskExplanationService;
    private final RiskAlertRepository riskAlertRepository;

    /**
     * Synchronous rule engine evaluation helper to keep engine execution logic encapsulated.
     */
    public RiskRuleResultDto evaluateRules(RiskContext context) {
        return riskRuleEngine.evaluate(context);
    }

    /**
     * Phase 1 (Synchronous): Saves the placeholder RiskAlert record synchronously within the 
     * transfer database transaction context to guarantee immediate persistence.
     */
    public RiskAlert createAlertSynchronously(RiskContext context, RiskRuleResultDto ruleResult) {
        log.info("[risk-analysis] creating placeholder alert synchronously transactionId={}", context.getTransactionId());
        
        RiskAlert alert = RiskAlert.builder()
                .transactionId(context.getTransactionId())
                .userId(context.getIdentitySignals() != null
                        ? context.getIdentitySignals().getFromUserId() : null)
                .riskLevel(RiskLevel.valueOf(ruleResult.getRiskLevel()))
                .riskScore(ruleResult.getRiskScore())
                .summary("Explanation pending AI analysis...")
                .reasonCodes(ruleResult.getTriggeredRules() != null
                        ? String.join("; ", ruleResult.getTriggeredRules()) : null)
                .riskStatus(RiskStatus.CREATED)
                .recommendedAction(deriveRecommendedAction(ruleResult.getRiskLevel()))
                .build();

        return riskAlertRepository.save(alert);
    }

    /**
     * Phase 2 (Asynchronous): Triggered out-of-band on a separate worker thread.
     * Hits the high-latency Spring AI LLM explanation engine and updates the record safely.
     */
    @Async
    public void generateExplanationAndUpdateAlertAsync(RiskAlert alert, RiskContext context, RiskRuleResultDto ruleResult) {
        try {
            log.info("[risk-analysis-async] start async explanation generation transactionId={} alertId={}",
                    context.getTransactionId(), alert.getId());
            
            String explanation = buildExplanation(context, ruleResult);
            
            RiskAlert persistedAlert = riskAlertRepository.findById(alert.getId())
                    .orElseThrow(() -> new IllegalStateException("Alert not found with id: " + alert.getId()));
            
            persistedAlert.setSummary(explanation.length() > 2000
                    ? explanation.substring(0, 2000) : explanation);
            
            riskAlertRepository.save(persistedAlert);
            
            log.info("[risk-analysis-async] updated alert with explanation transactionId={} alertId={}",
                    context.getTransactionId(), alert.getId());
        } catch (Exception e) {
            log.error("[risk-analysis-async] failed explanation update transactionId={} alertId={}",
                    context.getTransactionId(), alert.getId(), e);
        }
    }

    /**
     * Asynchronous entry point — remains for backward compatibility.
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
     * Synchronous entry point — kept intact for RiskDemoRunner and existing test setups.
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
