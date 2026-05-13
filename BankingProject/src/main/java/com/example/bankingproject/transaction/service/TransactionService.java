package com.example.bankingproject.transaction.service;

import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.risk.dto.TransactionRiskDataDto;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.entity.Transaction;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import com.example.bankingproject.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional
    public Transaction transfer(Long fromUserId, Long toUserId, BigDecimal amount){

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
        }
        catch(Exception e){
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw e;
        }
        return transactionRepository.save(tx);
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

    public TransactionRiskDataDto getTransactionRiskData(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        Integer recentTransactionCount = transactionRepository.countCompletedOutboundSince(userId, twentyFourHoursAgo);
        BigDecimal totalRecentTransactionAmount = transactionRepository.sumCompletedOutboundSince(userId, twentyFourHoursAgo);
        BigDecimal averageRecentTransactionAmount = transactionRepository.averageCompletedOutboundSince(userId, twentyFourHoursAgo);

        return TransactionRiskDataDto.builder()
                .recentTransactionCount(recentTransactionCount == null ? 0 : recentTransactionCount)
                .totalRecentTransactionAmount(totalRecentTransactionAmount)
                .averageRecentTransactionAmount(averageRecentTransactionAmount)
                .build();
    }

}
