package com.example.bankingproject.transaction.repository;

import com.example.bankingproject.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromUserIdOrToUserId(Long fromUserId,Long toUserId);
}
