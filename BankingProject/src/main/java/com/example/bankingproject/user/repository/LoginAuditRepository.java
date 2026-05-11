// src/main/java/com/example/bankingproject/user/repository/LoginAuditRepository.java

package com.example.bankingproject.user.repository;

import com.example.bankingproject.user.entity.LoginAudit;
import com.example.bankingproject.user.enums.LoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    @Query("""
        SELECT COUNT(la) FROM LoginAudit la 
        WHERE la.userId = :userId 
        AND la.status = 'FAILED'
        AND la.createdAt >= :since
    """)
    Integer countFailedLoginsSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    @Query("""
        SELECT la FROM LoginAudit la 
        WHERE la.userId = :userId 
        AND la.status = 'SUCCESS'
        ORDER BY la.createdAt DESC
        LIMIT 1
    """)
    LoginAudit findMostRecentSuccessfulLogin(@Param("userId") Long userId);

    List<LoginAudit> findByUserIdAndStatusAndCreatedAtGreaterThanOrderByCreatedAtDesc(
            Long userId,
            LoginStatus status,
            LocalDateTime since
    );
}