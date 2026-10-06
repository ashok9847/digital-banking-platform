package com.banking.common.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BankingException {
    public InsufficientBalanceException(String accountNumber, BigDecimal available, BigDecimal requested) {
        super(String.format("Account '%s' has insufficient balance. Available: %s, Requested: %s",
                accountNumber, available, requested), HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE");
    }

    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE");
    }
}
