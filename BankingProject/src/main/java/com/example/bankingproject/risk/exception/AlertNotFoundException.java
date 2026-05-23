package com.example.bankingproject.risk.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class AlertNotFoundException extends BankingException {
    public AlertNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND");
    }
}
