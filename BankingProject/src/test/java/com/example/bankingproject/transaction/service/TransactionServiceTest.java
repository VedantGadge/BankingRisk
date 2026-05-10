package com.example.bankingproject.transaction.service;

import com.example.bankingproject.account.dto.AccountResponse;
import com.example.bankingproject.account.service.AccountService;
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

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void transfer_shouldCompleteSuccessfully() {
        // Arrange
        long fromUserId = 1L;
        long toUserId = 2L;
        BigDecimal amount = new BigDecimal("100");

        // Mock transaction save to return the transaction with ID
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId(1L);
                    return tx;
                });

        // Mock account operations (don't throw exceptions)
        when(accountService.withdraw(fromUserId, amount))
                .thenReturn(AccountResponse.builder()
                        .userId(fromUserId)
                        .balance(new BigDecimal("400"))
                        .build());

        when(accountService.deposit(toUserId, amount))
                .thenReturn(AccountResponse.builder()
                        .userId(toUserId)
                        .balance(new BigDecimal("600"))
                        .build());

        // Act
        Transaction result = transactionService.transfer(fromUserId, toUserId, amount);

        // Assert
        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(fromUserId, result.getFromUserId());
        assertEquals(toUserId, result.getToUserId());
        assertEquals(0, result.getAmount().compareTo(amount));
        assertEquals(TransactionType.TRANSFER, result.getType());

        // Verify interactions - proves orchestration and inter-service coordination
        verify(accountService, times(1)).withdraw(fromUserId, amount);
        verify(accountService, times(1)).deposit(toUserId, amount);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

}

