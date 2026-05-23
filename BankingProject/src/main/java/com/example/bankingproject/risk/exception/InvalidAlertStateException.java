package com.example.bankingproject.risk.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class InvalidAlertStateException extends BankingException {
    public InvalidAlertStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_ALERT_STATE");
    }
}
