package com.banking.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionResponseDto {

    private Long id;
    private String transactionReference;
    private String idempotencyKey;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private String description;
    private String failureReason;
    private Instant createdAt;
    private Instant completedAt;

    public TransactionResponseDto() {}

    public TransactionResponseDto(Long id, String transactionReference, String idempotencyKey,
                                  String sourceAccountNumber, String destinationAccountNumber,
                                  BigDecimal amount, String currency, String transactionType,
                                  String status, String description, String failureReason,
                                  Instant createdAt, Instant completedAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
        this.status = status;
        this.description = description;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String transactionReference;
        private String idempotencyKey;
        private String sourceAccountNumber;
        private String destinationAccountNumber;
        private BigDecimal amount;
        private String currency;
        private String transactionType;
        private String status;
        private String description;
        private String failureReason;
        private Instant createdAt;
        private Instant completedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder transactionReference(String transactionReference) { this.transactionReference = transactionReference; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder sourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; return this; }
        public Builder destinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder transactionType(String transactionType) { this.transactionType = transactionType; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder completedAt(Instant completedAt) { this.completedAt = completedAt; return this; }

        public TransactionResponseDto build() {
            return new TransactionResponseDto(id, transactionReference, idempotencyKey, sourceAccountNumber,
                    destinationAccountNumber, amount, currency, transactionType, status, description, failureReason,
                    createdAt, completedAt);
        }
    }
}
