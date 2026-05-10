package com.example.bankingproject.risk.entity;

import com.example.bankingproject.risk.enums.RiskLevel;
import com.example.bankingproject.risk.enums.RiskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="risk_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAlert {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private Integer riskScore;

    @Column(length = 2000)
    private String summary;

    @Column(length = 4000)
    private String reasonCodes;

    @Enumerated(EnumType.STRING)
    private RiskStatus riskStatus;

    private String recommendedAction;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
