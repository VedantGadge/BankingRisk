package com.example.bankingproject.account.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND");
    }
}

