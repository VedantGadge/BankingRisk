package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskAnalysisResponse;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisService {

    private final RiskRuleEngine riskRuleEngine;
    private final RiskExplanationService riskExplanationService;

    public RiskAnalysisResponse analyze(RiskContext context) {
        log.info("[risk-analysis] start transactionId={} context={}",
                context != null ? context.getTransactionId() : null, context);

        RiskRuleResultDto ruleResult = riskRuleEngine.evaluate(context);
        log.info("[risk-analysis] rule-engine output transactionId={} result={}",
                context != null ? context.getTransactionId() : null, ruleResult);

        String explanation = riskExplanationService.explain(context, ruleResult);
        log.info("[risk-analysis] explanation built transactionId={} explanation={}",
                context != null ? context.getTransactionId() : null, explanation);

        RiskAnalysisResponse response = RiskAnalysisResponse.builder()
                .riskScore(ruleResult.getRiskScore())
                .riskLevel(ruleResult.getRiskLevel())
                .triggeredRules(ruleResult.getTriggeredRules())
                .explanation(explanation)
                .build();

        log.info("[risk-analysis] completed transactionId={} response={}",
                context != null ? context.getTransactionId() : null, response);
        return response;
    }
}