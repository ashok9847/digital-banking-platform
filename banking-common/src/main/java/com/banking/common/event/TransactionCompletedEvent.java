package com.banking.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event published when a financial transaction is successfully completed.
 * Does not contain sensitive credential information.
 */
public class TransactionCompletedEvent {

    private String eventId;
    private String transactionId;
    private String transactionType;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private String currency;
    private Long customerId;
    private Instant timestamp;

    public TransactionCompletedEvent() {
        this.timestamp = Instant.now();
    }

    public TransactionCompletedEvent(String eventId, String transactionId, String transactionType,
                                     String sourceAccount, String destinationAccount,
                                     BigDecimal amount, String currency, Long customerId,
                                     Instant timestamp) {
        this.eventId = eventId;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.customerId = customerId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }

    public String getDestinationAccount() { return destinationAccount; }
    public void setDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String transactionId;
        private String transactionType;
        private String sourceAccount;
        private String destinationAccount;
        private BigDecimal amount;
        private String currency;
        private Long customerId;
        private Instant timestamp = Instant.now();

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder transactionType(String transactionType) { this.transactionType = transactionType; return this; }
        public Builder sourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; return this; }
        public Builder destinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder customerId(Long customerId) { this.customerId = customerId; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public TransactionCompletedEvent build() {
            return new TransactionCompletedEvent(eventId, transactionId, transactionType, sourceAccount,
                    destinationAccount, amount, currency, customerId, timestamp);
        }
    }
}
