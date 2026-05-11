
package com.example.bankingproject.user.repository;

import com.example.bankingproject.user.entity.ProfileAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProfileAuditLogRepository extends JpaRepository<ProfileAuditLog, Long> {

    @Query("""
        SELECT COUNT(pal) FROM ProfileAuditLog pal 
        WHERE pal.userId = :userId 
        AND pal.createdAt >= :since
    """)
    Integer countProfileChangesSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    List<ProfileAuditLog> findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime createdAt
    );
}