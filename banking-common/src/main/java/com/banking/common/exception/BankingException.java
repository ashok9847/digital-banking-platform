package com.banking.common.exception;

import org.springframework.http.HttpStatus;

public class BankingException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BankingException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "BANKING_ERROR";
    }

    public BankingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "BANKING_ERROR";
    }

    public BankingException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
