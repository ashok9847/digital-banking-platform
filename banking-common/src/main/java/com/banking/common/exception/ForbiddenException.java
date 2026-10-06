package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BankingException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
