// src/main/java/com/example/bankingproject/transaction/repository/TransactionRepository.java

package com.example.bankingproject.transaction.repository;

import com.example.bankingproject.transaction.entity.Transaction;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByFromUserIdOrToUserIdOrderByCreatedAtDesc(
            Long fromUserId, Long toUserId, Pageable pageable );

    // COUNT COMPLETED OUTBOUND TRANSACTIONS SINCE TIME
    @Query("""
        SELECT COUNT(t) FROM Transaction t
        WHERE t.fromUserId = :fromUserId
        AND t.createdAt >= :since
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    Integer countCompletedOutboundSince(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

    // SUM OF COMPLETED OUTBOUND TRANSACTIONS SINCE TIME
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.fromUserId = :fromUserId
        AND t.createdAt >= :since
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    BigDecimal sumCompletedOutboundSince(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

    // AVERAGE OF COMPLETED OUTBOUND TRANSACTIONS SINCE TIME
    @Query("""
        SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t
        WHERE t.fromUserId = :fromUserId
        AND t.createdAt >= :since
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    BigDecimal averageCompletedOutboundSince(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

}