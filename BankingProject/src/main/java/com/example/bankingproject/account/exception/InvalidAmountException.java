package com.example.bankingproject.account.exception;

public class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(){
        super("Amount must be positive.");
    }
}
