package com.example.bankingproject.transaction.repository;

import com.example.bankingproject.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByFromUserIdOrToUserIdOrderByCreatedAtDesc(
            Long fromUserId, Long toUserId, Pageable pageable );
}
