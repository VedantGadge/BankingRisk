package com.example.bankingproject.transaction.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.risk.dto.RiskContext;
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

        // Create transaction (INITIATED)
        Transaction tx = Transaction.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.INITIATED)
                .build();

        // persist(save) the Transaction object in db
        tx = transactionRepository.save(tx);

        try{
            // perform money movement
            accountService.withdraw(fromUserId, amount);
            accountService.deposit(toUserId, amount);

            // mark success
            tx.setStatus(TransactionStatus.COMPLETED);
            tx = transactionRepository.save(tx);

            // Fire-and-forget: risk analysis runs asynchronously on a separate thread
            // so the transfer API returns immediately without waiting for the LLM call
            triggerAsyncRiskAnalysis(fromUserId, toUserId, amount, tx.getId());
        }
        catch(Exception e){
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw e;
        }
        return toResponse(tx);
    }

    /**
     * Triggers the risk analysis pipeline on a separate thread.
     * Errors are logged but never propagate to the caller.
     */
    private void triggerAsyncRiskAnalysis(Long fromUserId, Long toUserId, BigDecimal amount, Long transactionId) {
        try {
            RiskContext riskContext = riskContextBuilderService.buildRiskContext(
                    fromUserId, toUserId, amount, transactionId);
            riskAnalysisService.analyzeAsync(riskContext);
            log.info("[transfer-risk] async risk analysis triggered transactionId={}", transactionId);
        } catch (Exception riskException) {
            log.warn("[transfer-risk] failed to trigger risk pipeline transactionId={}", transactionId, riskException);
        }
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
