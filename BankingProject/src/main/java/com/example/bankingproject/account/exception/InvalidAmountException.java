package com.example.bankingproject.account.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class InvalidAmountException extends BankingException {
    public InvalidAmountException(){
        super("Amount must be positive.", HttpStatus.BAD_REQUEST, "INVALID_AMOUNT");
    }
}
