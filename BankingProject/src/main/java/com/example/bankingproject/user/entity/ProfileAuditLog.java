// src/main/java/com/example/bankingproject/user/entity/ProfileAuditLog.java

package com.example.bankingproject.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "profile_audit_logs", indexes = {
        @Index(name = "idx_profile_audit_user_timestamp", columnList = "user_id, created_at DESC")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProfileAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String changedField; // email, password, phone, address

    private String oldValue;
    private String newValue;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}