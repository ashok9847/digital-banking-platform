package com.banking.transaction.client;

import java.math.BigDecimal;

public class BalanceResultDto {
    private String accountNumber;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private String transactionReference;
    private String status;

    public BalanceResultDto() {}

    public BalanceResultDto(String accountNumber, BigDecimal previousBalance, BigDecimal newBalance, String transactionReference, String status) {
        this.accountNumber = accountNumber;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.transactionReference = transactionReference;
        this.status = status;
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
}
