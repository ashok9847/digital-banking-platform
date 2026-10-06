package com.banking.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class AccountResponseDto {

    private Long id;
    private String accountNumber;
    private Long customerId;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public AccountResponseDto() {}

    public AccountResponseDto(Long id, String accountNumber, Long customerId, String accountType,
                              BigDecimal balance, String currency, String status,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String accountNumber;
        private Long customerId;
        private String accountType;
        private BigDecimal balance;
        private String currency;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public Builder customerId(Long customerId) { this.customerId = customerId; return this; }
        public Builder accountType(String accountType) { this.accountType = accountType; return this; }
        public Builder balance(BigDecimal balance) { this.balance = balance; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public AccountResponseDto build() {
            return new AccountResponseDto(id, accountNumber, customerId, accountType, balance, currency, status, createdAt, updatedAt);
        }
    }
}
