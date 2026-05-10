package com.example.bankingproject.transaction.service;

import com.example.bankingproject.account.service.AccountService;
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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional
    public Transaction transfer(Long fromUserId, Long toUserId, BigDecimal amount){

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

            // mark failed
            tx.setStatus(TransactionStatus.FAILED);
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

}
