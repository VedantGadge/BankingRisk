package com.example.bankingproject.transaction.service;

import com.example.bankingproject.risk.dto.TransactionRiskDataDto;
import com.example.bankingproject.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionRiskQueryService {

    private final TransactionRepository transactionRepository;

    public TransactionRiskDataDto getTransactionRiskData(Long userId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        Integer recentTransactionCount =
                transactionRepository.countCompletedOutboundSince(userId, twentyFourHoursAgo);

        BigDecimal totalRecentTransactionAmount =
                transactionRepository.sumCompletedOutboundSince(userId, twentyFourHoursAgo);

        BigDecimal averageRecentTransactionAmount =
                transactionRepository.averageCompletedOutboundSince(userId, twentyFourHoursAgo);

        return TransactionRiskDataDto.builder()
                .recentTransactionCount(recentTransactionCount == null ? 0 : recentTransactionCount)
                .totalRecentTransactionAmount(totalRecentTransactionAmount)
                .averageRecentTransactionAmount(averageRecentTransactionAmount)
                .build();
    }
}