package com.example.bankingproject.risk.dto;

import com.example.bankingproject.risk.enums.RiskLevel;
import com.example.bankingproject.risk.enums.RiskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlertResponse {
    private Long id;
    private Long transactionId;
    private Long userId;
    private RiskLevel riskLevel;
    private Integer riskScore;
    private String summary;
    private String reasonCodes;
    private RiskStatus riskStatus;
    private String recommendedAction;
    private LocalDateTime createdAt;
}
