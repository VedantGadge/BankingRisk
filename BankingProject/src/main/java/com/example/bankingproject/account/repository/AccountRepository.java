package com.example.bankingproject.account.repository;

import com.example.bankingproject.account.entity.Account;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {

    Optional<Account> findByUserId (long userId);
    //Custom query method
    //Spring automatically generates SQL:
    //SELECT * FROM accounts WHERE user_id = ?
    //Optional is used because Account may or may not exist

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.userId = ?1") //@Lock annotation only works with explicit JPQL queries (using @Query)
    Optional<Account> findByUserIdForUpdate(long userId);
    //Uses pessimistic locking to prevent race conditions during concurrent withdrawals
    //Locks the row in the database until transaction completes

    // Add to AccountRepository


    @Query("""
        SELECT a.balance FROM Account a
        WHERE a.userId = :userId
    """)
    BigDecimal findBalanceByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT a.createdAt FROM Account a
        WHERE a.userId = :userId
    """)
    LocalDateTime findCreatedAtByUserId(@Param("userId") Long userId);
}
