package com.banking.audit.dto;

import java.time.Instant;

public class AuditLogResponseDto {

    private Long id;
    private String correlationId;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private String status;
    private String ipAddress;
    private String metadata;
    private Instant createdAt;

    public AuditLogResponseDto() {}

    public AuditLogResponseDto(Long id, String correlationId, String actor, String action,
                               String resourceType, String resourceId, String status,
                               String ipAddress, String metadata, Instant createdAt) {
        this.id = id;
        this.correlationId = correlationId;
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.status = status;
        this.ipAddress = ipAddress;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String correlationId;
        private String actor;
        private String action;
        private String resourceType;
        private String resourceId;
        private String status;
        private String ipAddress;
        private String metadata;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder actor(String actor) { this.actor = actor; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder resourceType(String resourceType) { this.resourceType = resourceType; return this; }
        public Builder resourceId(String resourceId) { this.resourceId = resourceId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AuditLogResponseDto build() {
            return new AuditLogResponseDto(id, correlationId, actor, action, resourceType, resourceId, status, ipAddress, metadata, createdAt);
        }
    }
}
