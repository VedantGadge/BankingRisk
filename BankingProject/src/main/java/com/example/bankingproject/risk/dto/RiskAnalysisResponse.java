package com.example.bankingproject.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAnalysisResponse {
    private Integer riskScore;
    private String riskLevel;
    private List<String> triggeredRules;
    private String explanation;
}