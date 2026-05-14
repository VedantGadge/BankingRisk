package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskAnalysisResponse;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import com.example.bankingproject.risk.repository.RiskAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

    @Mock
    private RiskRuleEngine riskRuleEngine;

    @Mock
    private RiskExplanationService riskExplanationService;

    @Mock
    private RiskAlertRepository riskAlertRepository;

    @InjectMocks
    private RiskAnalysisService riskAnalysisService;

    @Test
    void analyze_shouldSkipLlmForLowRisk() {
        RiskContext context = RiskContext.builder().transactionId(100L).build();
        RiskRuleResultDto ruleResult = RiskRuleResultDto.builder()
                .riskScore(10)
                .riskLevel("LOW")
                .triggeredRules(List.of("test rule"))
                .build();

        when(riskRuleEngine.evaluate(context)).thenReturn(ruleResult);

        RiskAnalysisResponse response = riskAnalysisService.analyze(context);

        assertEquals(10, response.getRiskScore());
        assertEquals("LOW", response.getRiskLevel());
        assertEquals("Low risk transaction. No LLM explanation generated.", response.getExplanation());
        verify(riskExplanationService, never()).explain(any(), any());
    }

    @Test
    void analyze_shouldCallLlmForMediumRisk() {
        RiskContext context = RiskContext.builder().transactionId(200L).build();
        RiskRuleResultDto ruleResult = RiskRuleResultDto.builder()
                .riskScore(55)
                .riskLevel("MEDIUM")
                .triggeredRules(List.of("profile changed"))
                .build();

        when(riskRuleEngine.evaluate(context)).thenReturn(ruleResult);
        when(riskExplanationService.explain(context, ruleResult)).thenReturn("LLM explanation");

        RiskAnalysisResponse response = riskAnalysisService.analyze(context);

        assertEquals(55, response.getRiskScore());
        assertEquals("MEDIUM", response.getRiskLevel());
        assertEquals("LLM explanation", response.getExplanation());
        verify(riskExplanationService, times(1)).explain(context, ruleResult);
    }
}

