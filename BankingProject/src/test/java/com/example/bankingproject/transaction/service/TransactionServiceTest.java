package com.example.bankingproject.transaction.service;

import com.example.bankingproject.account.dto.AccountResponse;
import com.example.bankingproject.account.exception.InsufficientBalanceException;
import com.example.bankingproject.account.service.AccountService;
import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.service.RiskAnalysisService;
import com.example.bankingproject.risk.service.RiskContextBuilderService;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.entity.Transaction;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import com.example.bankingproject.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountService accountService;
    @Mock private RiskContextBuilderService riskContextBuilderService;
    @Mock private RiskAnalysisService riskAnalysisService;

    @InjectMocks
    private TransactionService transactionService;

    // ─────────────────────────────────────────────────
    // Happy path
    // ─────────────────────────────────────────────────

    @Test
    void transfer_shouldCompleteSuccessfully() {
        long fromUserId = 1L;
        long toUserId = 2L;
        BigDecimal amount = new BigDecimal("100");

        RiskContext riskContext = RiskContext.builder().transactionId(1L).build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId(1L);
                    return tx;
                });

        when(accountService.withdraw(fromUserId, amount))
                .thenReturn(AccountResponse.builder().userId(fromUserId).balance(new BigDecimal("400")).build());

        when(accountService.deposit(toUserId, amount))
                .thenReturn(AccountResponse.builder().userId(toUserId).balance(new BigDecimal("600")).build());

        when(riskContextBuilderService.buildRiskContext(fromUserId, toUserId, amount, 1L))
                .thenReturn(riskContext);

        TransactionResponse result = transactionService.transfer(fromUserId, toUserId, amount);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(fromUserId, result.getFromUserId());
        assertEquals(toUserId, result.getToUserId());
        assertEquals(0, result.getAmount().compareTo(amount));
        assertEquals(TransactionType.TRANSFER, result.getType());

        verify(accountService, times(1)).withdraw(fromUserId, amount);
        verify(accountService, times(1)).deposit(toUserId, amount);
        verify(riskContextBuilderService, times(1)).buildRiskContext(fromUserId, toUserId, amount, 1L);
        verify(riskAnalysisService, times(1)).analyzeAsync(riskContext);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    // ─────────────────────────────────────────────────
    // Validation guards
    // ─────────────────────────────────────────────────

    @Test
    void transfer_shouldThrowWhenSenderAndReceiverAreSame() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(1L, 1L, new BigDecimal("100")));

        // No transaction should be saved, no money should move
        verify(transactionRepository, never()).save(any());
        verify(accountService, never()).withdraw(anyLong(), any());
    }

    @Test
    void transfer_shouldThrowOnNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(1L, 2L, null));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowOnZeroAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(1L, 2L, BigDecimal.ZERO));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_shouldThrowOnNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(1L, 2L, new BigDecimal("-50")));

        verify(transactionRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────
    // Failure path — money movement fails
    // ─────────────────────────────────────────────────

    @Test
    void transfer_shouldMarkFailedAndRethrowOnInsufficientBalance() {
        long fromUserId = 1L;
        long toUserId = 2L;
        BigDecimal amount = new BigDecimal("10000");

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId(1L);
                    return tx;
                });

        when(accountService.withdraw(fromUserId, amount))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.transfer(fromUserId, toUserId, amount));

        // Transaction must be saved twice: once INITIATED, once FAILED
        verify(transactionRepository, times(2)).save(argThat(tx -> {
            TransactionStatus status = tx.getStatus();
            return status == TransactionStatus.INITIATED || status == TransactionStatus.FAILED;
        }));

        // Deposit must never happen if withdrawal failed
        verify(accountService, never()).deposit(anyLong(), any());

        // Risk analysis must not run on failed transactions
        verify(riskAnalysisService, never()).analyzeAsync(any());
    }

    // ─────────────────────────────────────────────────
    // Risk pipeline isolation
    // ─────────────────────────────────────────────────

    @Test
    void transfer_shouldCompleteEvenIfRiskContextBuilderFails() {
        long fromUserId = 1L;
        long toUserId = 2L;
        BigDecimal amount = new BigDecimal("100");

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId(1L);
                    return tx;
                });

        when(accountService.withdraw(fromUserId, amount))
                .thenReturn(AccountResponse.builder().userId(fromUserId).balance(new BigDecimal("400")).build());

        when(accountService.deposit(toUserId, amount))
                .thenReturn(AccountResponse.builder().userId(toUserId).balance(new BigDecimal("600")).build());

        // Simulate risk context builder crashing
        when(riskContextBuilderService.buildRiskContext(fromUserId, toUserId, amount, 1L))
                .thenThrow(new RuntimeException("Risk service unavailable"));

        // Transfer must succeed — risk pipeline failure must never affect the transfer result
        TransactionResponse result = transactionService.transfer(fromUserId, toUserId, amount);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
    }
}
