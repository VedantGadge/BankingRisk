package com.example.bankingproject.common.ratelimit.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends BankingException {
    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }
}
