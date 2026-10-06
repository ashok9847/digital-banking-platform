package com.banking.common.event;

import java.time.Instant;

public class NotificationEvent {

    private String eventId;
    private Long customerId;
    private String transactionReference;
    private String notificationType;
    private String recipient;
    private String subject;
    private String message;
    private String correlationId;
    private Instant timestamp;

    public NotificationEvent() {
        this.timestamp = Instant.now();
    }

    public NotificationEvent(String eventId, Long customerId, String transactionReference,
                             String notificationType, String recipient, String subject,
                             String message, String correlationId, Instant timestamp) {
        this.eventId = eventId;
        this.customerId = customerId;
        this.transactionReference = transactionReference;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.correlationId = correlationId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private Long customerId;
        private String transactionReference;
        private String notificationType;
        private String recipient;
        private String subject;
        private String message;
        private String correlationId;
        private Instant timestamp = Instant.now();

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder customerId(Long customerId) { this.customerId = customerId; return this; }
        public Builder transactionReference(String transactionReference) { this.transactionReference = transactionReference; return this; }
        public Builder notificationType(String notificationType) { this.notificationType = notificationType; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public NotificationEvent build() {
            return new NotificationEvent(eventId, customerId, transactionReference, notificationType, recipient, subject, message, correlationId, timestamp);
        }
    }
}
