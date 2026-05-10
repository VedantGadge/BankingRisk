package com.example.bankingproject.account.service;

import com.example.bankingproject.account.dto.AccountResponse;
import com.example.bankingproject.account.entity.Account;
import com.example.bankingproject.account.exception.AccountNotFoundException;
import com.example.bankingproject.account.exception.InsufficientBalanceException;
import com.example.bankingproject.account.repository.AccountRepository;
import com.example.bankingproject.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_shouldSaveNewAccountWithZeroBalance() {
        Long userId = 1L;

        Account saved = Account.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        Account result = accountService.createAccount(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(0, result.getBalance().compareTo(BigDecimal.ZERO));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void getAccount_shouldReturnAccountWhenPresent() {
        long userId = 1L;
        Account account = Account.builder()
                .id(1L)
                .userId(userId)
                .balance(new BigDecimal("100.00"))
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        AccountResponse result = accountService.getAccount(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(0, result.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void getAccount_shouldThrowWhenMissing() {
        long userId = 1L;

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(userId));
    }

    @Test
    void deposit_shouldIncreaseBalance() {
        long userId = 1L;
        Account account = Account.builder()
                .id(1L)
                .userId(userId)
                .balance(new BigDecimal("100.00"))
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse result = accountService.deposit(userId, new BigDecimal("50.00"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("150.00")));
    }

    @Test
    void deposit_shouldThrowForNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(1L, BigDecimal.ZERO));
    }

    @Test
    void withdraw_shouldDecreaseBalance() {
        long userId = 1L;
        Account account = Account.builder()
                .id(1L)
                .userId(userId)
                .balance(new BigDecimal("100.00"))
                .build();

        when(accountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse result = accountService.withdraw(userId, new BigDecimal("40.00"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("60.00")));
    }

    @Test
    void withdraw_shouldAllowExactBalance() {
        long userId = 1L;
        Account account = Account.builder()
                .id(1L)
                .userId(userId)
                .balance(new BigDecimal("100.00"))
                .build();

        when(accountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse result = accountService.withdraw(userId, new BigDecimal("100.00"));

        assertEquals(0, result.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void withdraw_shouldThrowWhenBalanceIsInsufficient() {
        long userId = 1L;
        Account account = Account.builder()
                .id(1L)
                .userId(userId)
                .balance(new BigDecimal("50.00"))
                .build();

        when(accountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class,
                () -> accountService.withdraw(userId, new BigDecimal("100.00")));
    }

}
