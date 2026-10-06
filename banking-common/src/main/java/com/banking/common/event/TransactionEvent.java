package com.banking.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionEvent {

    private String eventId;
    private String transactionReference;
    private String idempotencyKey;
    private Long sourceAccountId;
    private String sourceAccountNumber;
    private Long destinationAccountId;
    private String destinationAccountNumber;
    private Long customerId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private String failureReason;
    private String correlationId;
    private Instant timestamp;

    public TransactionEvent() {
        this.timestamp = Instant.now();
    }

    public TransactionEvent(String eventId, String transactionReference, String idempotencyKey,
                            Long sourceAccountId, String sourceAccountNumber,
                            Long destinationAccountId, String destinationAccountNumber,
                            Long customerId, BigDecimal amount, String currency,
                            String transactionType, String status, String failureReason,
                            String correlationId, Instant timestamp) {
        this.eventId = eventId;
        this.transactionReference = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountId = destinationAccountId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
        this.status = status;
        this.failureReason = failureReason;
        this.correlationId = correlationId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getTransactionId() { return transactionReference; }
    public void setTransactionId(String transactionId) { this.transactionReference = transactionId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getSourceAccount() { return sourceAccountNumber; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccountNumber = sourceAccount; }

    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public String getDestinationAccount() { return destinationAccountNumber; }
    public void setDestinationAccount(String destinationAccount) { this.destinationAccountNumber = destinationAccount; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String transactionReference;
        private String idempotencyKey;
        private Long sourceAccountId;
        private String sourceAccountNumber;
        private Long destinationAccountId;
        private String destinationAccountNumber;
        private Long customerId;
        private BigDecimal amount;
        private String currency;
        private String transactionType;
        private String status;
        private String failureReason;
        private String correlationId;
        private Instant timestamp = Instant.now();

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder transactionReference(String transactionReference) { this.transactionReference = transactionReference; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public Builder sourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; return this; }
        public Builder sourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; return this; }
        public Builder destinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; return this; }
        public Builder destinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; return this; }
        public Builder customerId(Long customerId) { this.customerId = customerId; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder transactionType(String transactionType) { this.transactionType = transactionType; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public TransactionEvent build() {
            return new TransactionEvent(eventId, transactionReference, idempotencyKey, sourceAccountId,
                    sourceAccountNumber, destinationAccountId, destinationAccountNumber, customerId,
                    amount, currency, transactionType, status, failureReason, correlationId, timestamp);
        }
    }
}
