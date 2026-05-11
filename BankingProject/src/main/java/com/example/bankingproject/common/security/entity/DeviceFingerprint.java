// src/main/java/com/example/bankingproject/common/security/entity/DeviceFingerprint.java

package com.example.bankingproject.common.security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_fingerprints", indexes = {
        @Index(name = "idx_user_device", columnList = "user_id, device_id"),
        @Index(name = "idx_user_last_seen", columnList = "user_id, last_seen_at DESC")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String deviceId; // hash of userAgent + IP

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    private String country;
    private String city;
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    private Integer riskScore; // 0-100, based on VPN, suspicious IP, etc.

    private Boolean isVpn; // detected VPN/proxy
    private Boolean isSuspicious;

    @PrePersist
    public void prePersist() {
        if (firstSeenAt == null) {
            this.firstSeenAt = LocalDateTime.now();
        }
        this.lastSeenAt = LocalDateTime.now();
    }
}