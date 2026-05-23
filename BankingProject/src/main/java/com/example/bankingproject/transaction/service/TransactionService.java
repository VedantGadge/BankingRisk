package com.example.bankingproject.transaction.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import com.example.bankingproject.risk.entity.RiskAlert;
import com.example.bankingproject.risk.enums.RiskStatus;
import com.example.bankingproject.risk.exception.AlertNotFoundException;
import com.example.bankingproject.risk.exception.InvalidAlertStateException;
import com.example.bankingproject.risk.repository.RiskAlertRepository;
import com.example.bankingproject.risk.service.RiskAnalysisService;
import com.example.bankingproject.risk.service.RiskContextBuilderService;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.entity.Transaction;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import com.example.bankingproject.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final RiskContextBuilderService riskContextBuilderService;
    private final RiskAnalysisService riskAnalysisService;
    private final RiskAlertRepository riskAlertRepository;

    @Transactional
    public TransactionResponse transfer(Long fromUserId, Long toUserId, BigDecimal amount){

        if (fromUserId == null || toUserId == null) {
            throw new IllegalArgumentException("User IDs cannot be null");
        }
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Sender and receiver cannot be the same");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // 1. Create transaction in INITIATED status
        Transaction tx = Transaction.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.INITIATED)
                .build();

        tx = transactionRepository.save(tx);

        RiskContext riskContext = null;
        RiskRuleResultDto ruleResult = null;
        RiskAlert alert = null;

        try {
            // 2. Synchronously compile Risk Context & evaluate rules
            riskContext = riskContextBuilderService.buildRiskContext(fromUserId, toUserId, amount, tx.getId());
            ruleResult = riskAnalysisService.evaluateRules(riskContext);
            
            // 3. Persist the Risk Alert synchronously as a transaction boundary guarantee
            alert = riskAnalysisService.createAlertSynchronously(riskContext, ruleResult);
        } catch (Exception riskException) {
            log.warn("[transfer-risk] Risk evaluation failed, falling back to default low-risk path transactionId={}", tx.getId(), riskException);
        }

        try {
            if (ruleResult != null && "HIGH".equalsIgnoreCase(ruleResult.getRiskLevel())) {
                log.warn("[transfer-hold] HIGH risk transaction intercepted. Holding funds. transactionId={}", tx.getId());
                
                // Debit sender account immediately (Reserve/Escrow Hold)
                accountService.withdraw(fromUserId, amount);

                tx.setStatus(TransactionStatus.PENDING_REVIEW);
                tx = transactionRepository.save(tx);

                // Dispatch LLM task asynchronously
                riskAnalysisService.generateExplanationAndUpdateAlertAsync(alert, riskContext, ruleResult);

                return toResponse(tx);
            }

            // 4. Default Path (LOW / MEDIUM)
            accountService.withdraw(fromUserId, amount);
            accountService.deposit(toUserId, amount);

            tx.setStatus(TransactionStatus.COMPLETED);
            tx = transactionRepository.save(tx);

            // Asynchronously build descriptions for MEDIUM/auditable logs
            if (ruleResult != null && alert != null && "MEDIUM".equalsIgnoreCase(ruleResult.getRiskLevel())) {
                riskAnalysisService.generateExplanationAndUpdateAlertAsync(alert, riskContext, ruleResult);
            }

        } catch (Exception e) {
            log.error("[transfer-failed] failed execution transactionId={}", tx.getId(), e);
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw e;
        }

        return toResponse(tx);
    }

    /**
     * Compliance analyst approves a held transaction:
     * - Withdraw was performed at initiation. Credit receiver's account now.
     * - Status updates: Transaction -> COMPLETED, Risk Alert -> APPROVED.
     */
    @Transactional
    public TransactionResponse approvePendingTransaction(Long alertId, String analystNotes) {
        log.info("[triage-resolve] executing APPROVAL for alertId={}", alertId);

        RiskAlert alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + alertId));

        if (alert.getRiskStatus() != RiskStatus.CREATED && alert.getRiskStatus() != RiskStatus.UNDER_REVIEW) {
            throw new InvalidAlertStateException("Alert has already been resolved or processed.");
        }

        Transaction tx = transactionRepository.findById(alert.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for ID: " + alert.getTransactionId()));

        if (tx.getStatus() != TransactionStatus.PENDING_REVIEW) {
            throw new InvalidAlertStateException("Associated transaction is not in PENDING_REVIEW status.");
        }

        // Funds already debited from sender. Credit receiver now.
        accountService.deposit(tx.getToUserId(), tx.getAmount());

        tx.setStatus(TransactionStatus.COMPLETED);
        tx = transactionRepository.save(tx);

        alert.setRiskStatus(RiskStatus.APPROVED);
        alert.setSummary(alert.getSummary() + "\n\n[Triage Resolution Notes]: " + analystNotes);
        riskAlertRepository.save(alert);

        log.info("[triage-resolve] transactionId={} approved and settled", tx.getId());
        return toResponse(tx);
    }

    /**
     * Compliance analyst rejects a held transaction:
     * - Refund the reserved funds back to the sender's account.
     * - Status updates: Transaction -> FAILED, Risk Alert -> REJECTED.
     */
    @Transactional
    public TransactionResponse rejectPendingTransaction(Long alertId, String analystNotes) {
        log.info("[triage-resolve] executing REJECTION for alertId={}", alertId);

        RiskAlert alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + alertId));

        if (alert.getRiskStatus() != RiskStatus.CREATED && alert.getRiskStatus() != RiskStatus.UNDER_REVIEW) {
            throw new InvalidAlertStateException("Alert has already been resolved or processed.");
        }

        Transaction tx = transactionRepository.findById(alert.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for ID: " + alert.getTransactionId()));

        if (tx.getStatus() != TransactionStatus.PENDING_REVIEW) {
            throw new InvalidAlertStateException("Associated transaction is not in PENDING_REVIEW status.");
        }

        // Refund reserved funds back to the sender
        accountService.deposit(tx.getFromUserId(), tx.getAmount());

        tx.setStatus(TransactionStatus.FAILED);
        tx = transactionRepository.save(tx);

        alert.setRiskStatus(RiskStatus.REJECTED);
        alert.setSummary(alert.getSummary() + "\n\n[Triage Resolution Notes]: " + analystNotes);
        riskAlertRepository.save(alert);

        log.info("[triage-resolve] transactionId={} rejected and sender refunded", tx.getId());
        return toResponse(tx);
    }

    public Page<TransactionResponse> getHistory(Long userId, int page, int size){
        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page,size);

        return transactionRepository
                .findByFromUserIdOrToUserIdOrderByCreatedAtDesc(userId,userId,pageable)
                .map(this::toResponse);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .fromUserId(tx.getFromUserId())
                .toUserId(tx.getToUserId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
