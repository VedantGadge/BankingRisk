package com.example.bankingproject.common.idempotency.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class DuplicateRequestException extends BankingException {
    public DuplicateRequestException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_REQUEST");
    }
}
