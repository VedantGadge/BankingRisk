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

    // COUNT DISTINCT RECIPIENTS (FAN-OUT)
    @Query("""
        SELECT COUNT(DISTINCT t.toUserId) FROM Transaction t 
        WHERE t.fromUserId = :fromUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    Integer countDistinctRecipients(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

    // COUNT DISTINCT SENDERS (FAN-IN)
    @Query("""
        SELECT COUNT(DISTINCT t.fromUserId) FROM Transaction t 
        WHERE t.toUserId = :toUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.fromUserId IS NOT NULL
    """)
    Integer countDistinctSenders(
            @Param("toUserId") Long toUserId,
            @Param("since") LocalDateTime since
    );

    // SUM OUTBOUND 24H
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t 
        WHERE t.fromUserId = :fromUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    BigDecimal sumOutboundSince(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

    //  SUM INBOUND 24H
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t 
        WHERE t.toUserId = :toUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.fromUserId IS NOT NULL
    """)
    BigDecimal sumInboundSince(
            @Param("toUserId") Long toUserId,
            @Param("since") LocalDateTime since
    );

    // GET ALL RECIPIENTS IN WINDOW
    @Query("""
        SELECT DISTINCT t.toUserId FROM Transaction t 
        WHERE t.fromUserId = :fromUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    List<Long> findRecipientsInWindow(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

    // COUNT OUTBOUND TRANSFERS IN WINDOW
    @Query("""
        SELECT COUNT(t) FROM Transaction t 
        WHERE t.fromUserId = :fromUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
    """)
    Integer countOutboundTransactionsInWindow(
            @Param("fromUserId") Long fromUserId,
            @Param("since") LocalDateTime since
    );

    //  FIND MOST RECENT INBOUND
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.toUserId = :toUserId 
        AND t.createdAt >= :since 
        AND t.status = 'COMPLETED'
        AND t.fromUserId IS NOT NULL
        ORDER BY t.createdAt DESC
        LIMIT 1
    """)
    Optional<Transaction> findMostRecentInbound(
            @Param("toUserId") Long toUserId,
            @Param("since") LocalDateTime since
    );

    //  FIND OUTBOUND TRANSACTIONS IN WINDOW
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.fromUserId = :fromUserId 
        AND t.createdAt BETWEEN :start AND :end 
        AND t.status = 'COMPLETED'
        AND t.toUserId IS NOT NULL
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findOutboundTransactionsInWindow(
            @Param("fromUserId") Long fromUserId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}