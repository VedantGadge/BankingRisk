package com.example.bankingproject.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all business-logic errors in the Banking Application.
 * Having a common hierarchy allows the GlobalExceptionHandler to handle
 * all domain errors in a single generic block.
 */
@Getter
public abstract class BankingException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected BankingException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
