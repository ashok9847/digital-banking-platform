package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BankingException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
