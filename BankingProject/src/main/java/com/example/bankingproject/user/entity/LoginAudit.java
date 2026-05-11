// src/main/java/com/example/bankingproject/user/entity/LoginAudit.java

package com.example.bankingproject.user.entity;

import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.user.enums.LoginStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_audits", indexes = {
        @Index(name = "idx_user_timestamp", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoginStatus status; // SUCCESS, FAILED, BLOCKED

    private String ipAddress;
    private String userAgent;
    private String failureReason; // e.g., "INVALID_PASSWORD", "ACCOUNT_LOCKED"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}