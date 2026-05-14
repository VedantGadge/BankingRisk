package com.example.bankingproject.risk.repository;

import com.example.bankingproject.risk.entity.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
}
