package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends BankingException {
    public InvalidTransactionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION");
    }

    public InvalidTransactionException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
