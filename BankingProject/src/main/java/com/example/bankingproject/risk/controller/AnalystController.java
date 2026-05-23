package com.example.bankingproject.risk.controller;

import com.example.bankingproject.risk.dto.ResolveAlertRequest;
import com.example.bankingproject.risk.dto.RiskAlertResponse;
import com.example.bankingproject.risk.entity.RiskAlert;
import com.example.bankingproject.risk.repository.RiskAlertRepository;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyst")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analyst", description = "Compliance Analyst triage and risk alert APIs")
public class AnalystController {

    private final RiskAlertRepository riskAlertRepository;
    private final TransactionService transactionService;

    @Operation(summary = "Get paginated risk alerts, ordered by creation date (newest first)")
    @GetMapping("/alerts")
    public Page<RiskAlertResponse> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[analyst-controller] fetching risk alerts page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<RiskAlert> alerts = riskAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
        return alerts.map(this::toResponse);
    }

    @Operation(summary = "Resolve a pending risk alert (APPROVE or REJECT)")
    @PostMapping("/alerts/{alertId}/resolve")
    public TransactionResponse resolveAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody ResolveAlertRequest request
    ) {
        log.info("[analyst-controller] resolving alert alertId={}, action={}, notes={}",
                alertId, request.getAction(), request.getNotes());

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            return transactionService.approvePendingTransaction(alertId, request.getNotes());
        } else {
            return transactionService.rejectPendingTransaction(alertId, request.getNotes());
        }
    }

    private RiskAlertResponse toResponse(RiskAlert alert) {
        return RiskAlertResponse.builder()
                .id(alert.getId())
                .transactionId(alert.getTransactionId())
                .userId(alert.getUserId())
                .riskLevel(alert.getRiskLevel())
                .riskScore(alert.getRiskScore())
                .summary(alert.getSummary())
                .reasonCodes(alert.getReasonCodes())
                .riskStatus(alert.getRiskStatus())
                .recommendedAction(alert.getRecommendedAction())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
