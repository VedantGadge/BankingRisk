package com.example.bankingproject.account.exception;

import com.example.bankingproject.common.exception.BankingException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BankingException {
    public UserNotFoundException(){
        super("User not found.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }
}
