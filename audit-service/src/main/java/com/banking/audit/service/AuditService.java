package com.banking.audit.service;

import com.banking.audit.dto.AuditLogResponseDto;
import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import com.banking.common.event.AuditEvent;
import com.banking.common.event.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditLog recordAuditEvent(AuditEvent event) {
        log.info("Recording audit log: actor={}, action={}, resource={}/{}",
                event.getActor(), event.getAction(), event.getResourceType(), event.getResourceId());

        if (event.getCorrelationId() != null && !event.getCorrelationId().isBlank()) {
            java.util.Optional<AuditLog> existing = auditLogRepository
                    .findFirstByCorrelationIdAndActionAndResourceTypeAndResourceId(
                            event.getCorrelationId(), event.getAction(), event.getResourceType(), event.getResourceId());
            if (existing.isPresent()) {
                log.warn("Duplicate audit event detected for correlationId [{}], action [{}]. Skipping duplicate entry.",
                        event.getCorrelationId(), event.getAction());
                return existing.get();
            }
        }

        String sanitizedMetadata = sanitizeMetadata(event.getMetadata());

        AuditLog auditLog = new AuditLog(
                event.getCorrelationId(),
                event.getActor() != null ? event.getActor() : "SYSTEM",
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getStatus(),
                event.getIpAddress(),
                sanitizedMetadata
        );

        return auditLogRepository.save(auditLog);
    }

    @Transactional
    public AuditLog recordTransactionEvent(TransactionEvent event) {
        log.info("Recording transaction audit log: txRef={}, type={}, status={}",
                event.getTransactionReference(), event.getTransactionType(), event.getStatus());

        String action = "FINANCIAL_TRANSACTION_" + event.getTransactionType();
        String resourceId = event.getTransactionReference();
        String status = event.getStatus();

        if (resourceId != null && !resourceId.isBlank()) {
            java.util.Optional<AuditLog> existing = auditLogRepository
                    .findFirstByActionAndResourceTypeAndResourceIdAndStatus(action, "TRANSACTION", resourceId, status);
            if (existing.isPresent()) {
                log.warn("Duplicate transaction audit event detected for txRef [{}], status [{}]. Skipping duplicate entry.",
                        resourceId, status);
                return existing.get();
            }
        }

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            metadataJson = "{}";
        }
        metadataJson = sanitizeMetadata(metadataJson);

        String actor = (event.getCustomerId() != null) ?
                "CUSTOMER-" + event.getCustomerId() : "ACCOUNT-" + event.getSourceAccountNumber();

        AuditLog auditLog = new AuditLog(
                event.getCorrelationId(),
                actor,
                action,
                "TRANSACTION",
                resourceId,
                status,
                null,
                metadataJson
        );

        return auditLogRepository.save(auditLog);
    }

    private String sanitizeMetadata(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        return raw.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***REDACTED***$2")
                  .replaceAll("(?i)(\"secret\"\\s*:\\s*\")[^\"]*(\")", "$1***REDACTED***$2")
                  .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1***REDACTED***$2");
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> searchAuditLogs(String actor, String action, String resourceType, Pageable pageable) {
        return auditLogRepository.searchLogs(actor, action, resourceType, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getResourceHistory(String resourceType, String resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AuditLogResponseDto mapToDto(AuditLog log) {
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .correlationId(log.getCorrelationId())
                .actor(log.getActor())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .status(log.getStatus())
                .ipAddress(log.getIpAddress())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
