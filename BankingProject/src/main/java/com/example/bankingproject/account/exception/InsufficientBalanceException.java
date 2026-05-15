package com.example.bankingproject.account.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends BankingException {
    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE");
    }
}

