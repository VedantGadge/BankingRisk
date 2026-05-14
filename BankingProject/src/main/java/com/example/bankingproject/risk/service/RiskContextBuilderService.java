package com.example.bankingproject.risk.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.risk.dto.BehaviorRiskDataDto;
import com.example.bankingproject.risk.dto.IdentityRiskDataDto;
import com.example.bankingproject.risk.dto.MoneyFlowRiskDataDto;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.TransactionRiskDataDto;
import com.example.bankingproject.transaction.service.TransactionRiskQueryService;
import com.example.bankingproject.user.repository.LoginAuditRepository;
import com.example.bankingproject.user.repository.ProfileAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskContextBuilderService {

    // Pulls raw data from the transaction, account, and user-audit layers.
    private final TransactionRiskQueryService transactionRiskQueryService;
    private final AccountService accountService;
    private final LoginAuditRepository loginAuditRepository;
    private final ProfileAuditLogRepository profileAuditLogRepository;

    public RiskContext buildRiskContext(Long fromUserId, Long toUserId, BigDecimal amount, Long transactionId) {
        log.info("[risk-context] start build transactionId={}, fromUserId={}, toUserId={}, amount={}",
                transactionId, fromUserId, toUserId, amount);

        // Use a rolling 24-hour window for the recent activity signals.
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        // Identity signals: who is sending, who is receiving, how old is the account, and whether the profile changed recently.
        Long accountAgeDays = accountService.getAccountAgeDays(fromUserId);
        Boolean profileRecentlyChanged =
                profileAuditLogRepository.countProfileChangesSince(fromUserId, twentyFourHoursAgo) > 0;
        log.debug("[risk-context] identity source values transactionId={}, accountAgeDays={}, profileRecentlyChanged={}",
                transactionId, accountAgeDays, profileRecentlyChanged);

        IdentityRiskDataDto identity = IdentityRiskDataDto.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .accountAgeDays(accountAgeDays)
                .profileRecentlyChanged(profileRecentlyChanged)
                .build();

        // Behavior signals: recent transfer activity plus failed login count.
        TransactionRiskDataDto txData =
                transactionRiskQueryService.getTransactionRiskData(fromUserId);        Integer failedLoginCount = loginAuditRepository.countFailedLoginsSince(fromUserId, twentyFourHoursAgo);
        log.debug("[risk-context] behavior source values transactionId={}, txData={}, failedLoginCount={}",
                transactionId, txData, failedLoginCount);

        BehaviorRiskDataDto behavior = BehaviorRiskDataDto.builder()
                .recentTransactionCount(txData.getRecentTransactionCount())
                .totalRecentTransactionAmount(txData.getTotalRecentTransactionAmount())
                .averageRecentTransactionAmount(txData.getAverageRecentTransactionAmount())
                .failedLoginCount(failedLoginCount == null ? 0 : failedLoginCount)
                .build();

        // Money-flow signals: current balance and how large the transfer is compared to that balance.
        BigDecimal currentBalance = accountService.getCurrentBalance(fromUserId);
        BigDecimal amountToBalanceRatio = accountService.calculateAmountToBalanceRatio(fromUserId, amount);
        log.debug("[risk-context] money-flow source values transactionId={}, currentBalance={}, amountToBalanceRatio={}",
                transactionId, currentBalance, amountToBalanceRatio);

        MoneyFlowRiskDataDto moneyFlow = MoneyFlowRiskDataDto.builder()
                .transactionAmount(amount)
                .currentBalance(currentBalance)
                .amountToBalanceRatio(amountToBalanceRatio)
                .build();

        RiskContext riskContext = RiskContext.builder()
                .transactionId(transactionId)
                .identitySignals(toIdentitySignals(identity))
                .behaviorSignals(toBehaviorSignals(behavior))
                .moneyFlowSignals(toMoneyFlowSignals(moneyFlow))
                .build();

        log.info("[risk-context] built transactionId={}, riskContext={}", transactionId, riskContext);
        return riskContext;
    }

    private RiskContext.IdentitySignals toIdentitySignals(IdentityRiskDataDto dto) {
        return RiskContext.IdentitySignals.builder()
                .fromUserId(dto.getFromUserId())
                .toUserId(dto.getToUserId())
                .accountAgeDays(dto.getAccountAgeDays())
                .profileRecentlyChanged(dto.getProfileRecentlyChanged())
                .build();
    }

    private RiskContext.BehaviorSignals toBehaviorSignals(BehaviorRiskDataDto dto) {
        return RiskContext.BehaviorSignals.builder()
                .recentTransactionCount(dto.getRecentTransactionCount())
                .totalRecentTransactionAmount(dto.getTotalRecentTransactionAmount())
                .averageRecentTransactionAmount(dto.getAverageRecentTransactionAmount())
                .failedLoginCount(dto.getFailedLoginCount())
                .build();
    }

    private RiskContext.MoneyFlowSignals toMoneyFlowSignals(MoneyFlowRiskDataDto dto) {
        return RiskContext.MoneyFlowSignals.builder()
                .transactionAmount(dto.getTransactionAmount())
                .currentBalance(dto.getCurrentBalance())
                .amountToBalanceRatio(dto.getAmountToBalanceRatio())
                .build();
    }

    /*
     * Summary:
     * - Location in flow: this service sits in the middle of the risk pipeline.
     * - Input: receives transfer details (fromUserId, toUserId, amount, transactionId).
     * - Data sources: reads transaction history, account balance/age, login audits, and profile audit logs.
     * - Output: builds a slim RiskContext containing IdentitySignals, BehaviorSignals, and MoneyFlowSignals.
     * - Purpose: centralizes all risk-data collection so the fraud/risk layer can evaluate one clean object.
     */
}