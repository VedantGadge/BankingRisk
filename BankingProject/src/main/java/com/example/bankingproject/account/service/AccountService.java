package com.example.bankingproject.account.service;

import com.example.bankingproject.account.entity.Account;
import com.example.bankingproject.account.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;

    // to get account by userId
    public Account getAccount(Long userId){
        return accountRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Account not found!"));
    }

    public Account createAccount(Long userId){
        Account account = Account.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();

        return accountRepo.save(account);
    }

    @Transactional
    public Account deposit(long userId, BigDecimal amount){

        if(amount.compareTo(BigDecimal.ZERO)<0){
            throw new RuntimeException("Amount must be positive.");
        }

        Account account = getAccount(userId);
        account.setBalance(account.getBalance().add(amount));
        return accountRepo.save(account);

    }

    @Transactional
    public Account withdraw(long userId, BigDecimal amount){

        if(amount.compareTo(BigDecimal.ZERO)<0){
            throw new RuntimeException("Amount must be positive.");
        }

        Account account = getAccount(userId);

        if(account.getBalance().compareTo(amount)<=0){
            throw new RuntimeException("Insufficient Balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        return accountRepo.save(account);

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

 */
