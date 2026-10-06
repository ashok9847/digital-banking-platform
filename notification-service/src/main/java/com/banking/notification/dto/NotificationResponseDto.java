package com.banking.notification.dto;

import java.time.Instant;

public class NotificationResponseDto {

    private Long id;
    private Long customerId;
    private String transactionReference;
    private String notificationType;
    private String recipient;
    private String subject;
    private String message;
    private String status;
    private Instant createdAt;

    public NotificationResponseDto() {}

    public NotificationResponseDto(Long id, Long customerId, String transactionReference,
                                   String notificationType, String recipient, String subject,
                                   String message, String status, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.transactionReference = transactionReference;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long customerId;
        private String transactionReference;
        private String notificationType;
        private String recipient;
        private String subject;
        private String message;
        private String status;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder customerId(Long customerId) { this.customerId = customerId; return this; }
        public Builder transactionReference(String transactionReference) { this.transactionReference = transactionReference; return this; }
        public Builder notificationType(String notificationType) { this.notificationType = notificationType; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public NotificationResponseDto build() {
            return new NotificationResponseDto(id, customerId, transactionReference, notificationType, recipient, subject, message, status, createdAt);
        }
    }
}
