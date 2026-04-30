package com.example.bankingproject.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountRequest {

    @NotNull
    @DecimalMin(value="0.01", message = "Amount must be positive")
    private BigDecimal amount;
}


/*
This is better to use than using ap<String, BigDecimal> request in repository because:
1) Built-in validation
2) Type safety
3) Cleaner API contract , structured and predictable
4) Scalable
 */
