package com.banking.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class BalanceUpdateResponse {

    private String accountNumber;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private String transactionReference;
    private String status;
    private Instant timestamp;

    public BalanceUpdateResponse() {
        this.timestamp = Instant.now();
    }

    public BalanceUpdateResponse(String accountNumber, BigDecimal previousBalance,
                                 BigDecimal newBalance, String transactionReference, String status) {
        this.accountNumber = accountNumber;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.transactionReference = transactionReference;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(BigDecimal previousBalance) { this.previousBalance = previousBalance; }

    public BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
