package com.example.bankingproject.account.service;

import com.example.bankingproject.account.dto.AccountResponse;
import com.example.bankingproject.account.entity.Account;
import com.example.bankingproject.account.exception.AccountNotFoundException;
import com.example.bankingproject.account.exception.InsufficientBalanceException;
import com.example.bankingproject.account.repository.AccountRepository;
import com.example.bankingproject.transaction.entity.Transaction;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import com.example.bankingproject.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;

    // to get account by userId
    public AccountResponse getAccount(Long userId){
        log.info("Fetching account for userId: {}", userId);
        Account account = accountRepo.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Account not found for userId: {}", userId);
                    return new AccountNotFoundException("Account not found for userId: " + userId);
                });
        return mapToResponse(account);
    }

    public Account createAccount(Long userId){
        log.info("Creating new account for userId: {}", userId);
        Account account = Account.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();

        Account savedAccount = accountRepo.save(account);
        log.info("Account created successfully for userId: {}", userId);
        return savedAccount;
    }

    @Transactional
    public AccountResponse deposit(long userId, BigDecimal amount){
        log.info("User {} attempting deposit of {}", userId, amount);

        Account account = getAccountInternal(userId);
        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepo.save(account);

        // Create transaction record for deposit
        Transaction tx = Transaction.builder()
                .toUserId(userId)
                .fromUserId(null) // null for deposits (no sender)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepo.save(tx);

        log.info("Deposit successful for userId: {}, new balance: {}", userId, updatedAccount.getBalance());
        return mapToResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse withdraw(long userId, BigDecimal amount){
        log.info("User {} attempting withdrawal of {}", userId, amount);

        // Use pessimistic locking to prevent race conditions
        Account account = accountRepo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> {
                    log.error("Account not found for userId: {}", userId);
                    return new AccountNotFoundException("Account not found for userId: " + userId);
                });

        // BUG FIX: Changed from <= to < to allow exact balance withdrawal
        if(account.getBalance().compareTo(amount) < 0){
            log.error("Insufficient balance for userId: {}. Current: {}, Requested: {}",
                    userId, account.getBalance(), amount);
            throw new InsufficientBalanceException(
                    "Insufficient balance. Current: " + account.getBalance() + ", Requested: " + amount
            );
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepo.save(account);

        // Create transaction record for withdrawal
        Transaction tx = Transaction.builder()
                .fromUserId(userId)
                .toUserId(null) // null for withdrawals (no recipient)
                .amount(amount)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepo.save(tx);

        log.info("Withdrawal successful for userId: {}, new balance: {}", userId, updatedAccount.getBalance());
        return mapToResponse(updatedAccount);
    }

    // Internal method to fetch account without logging (avoid duplicate logs)
    private Account getAccountInternal(Long userId){
        return accountRepo.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for userId: " + userId));
    }

    // Convert Account entity to response DTO
    private AccountResponse mapToResponse(Account account){
        return AccountResponse.builder()
                .userId(account.getUserId())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}

/*

@Transaction:
Ensures:
✔ All DB operations succeed OR fail together
✔ Prevents inconsistent state


a.compareTo(b) returns:
Value	Meaning
> 0	     a > b
= 0	     a == b
< 0	     a < b

PESSIMISTIC_WRITE Lock:
✔ Prevents race conditions in concurrent operations
✔ Database holds lock until transaction completes
✔ Critical for financial transactions

 */
