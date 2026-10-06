package com.banking.audit.service;

import com.banking.audit.dto.AuditLogResponseDto;
import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import com.banking.common.event.AuditEvent;
import com.banking.common.event.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditService auditService;

    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = new AuditLog(
                "corr-123",
                "ADMIN_USER",
                "ACCOUNT_STATUS_CHANGE",
                "ACCOUNT",
                "ACC100200",
                "SUCCESS",
                "127.0.0.1",
                "{\"status\":\"BLOCKED\"}"
        );
        sampleLog.setId(1L);
    }

    @Test
    @DisplayName("Should record generic audit event successfully")
    void recordAuditEvent_Success() {
        AuditEvent event = AuditEvent.builder()
                .correlationId("corr-123")
                .actor("ADMIN_USER")
                .action("ACCOUNT_STATUS_CHANGE")
                .resourceType("ACCOUNT")
                .resourceId("ACC100200")
                .status("SUCCESS")
                .ipAddress("127.0.0.1")
                .metadata("{\"status\":\"BLOCKED\"}")
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        AuditLog saved = auditService.recordAuditEvent(event);

        assertNotNull(saved);
        assertEquals("ACCOUNT_STATUS_CHANGE", saved.getAction());
        assertEquals("ACC100200", saved.getResourceId());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should record transaction event in audit trail")
    void recordTransactionEvent_Success() {
        TransactionEvent txEvent = TransactionEvent.builder()
                .eventId("EVT-1")
                .correlationId("corr-tx")
                .transactionReference("TXN-998877")
                .customerId(50L)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .transactionType("TRANSFER")
                .status("SUCCESS")
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        AuditLog saved = auditService.recordTransactionEvent(txEvent);

        assertNotNull(saved);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should detect duplicate audit event and skip saving")
    void recordAuditEvent_Duplicate_ReturnsExistingWithoutSaving() {
        AuditEvent event = AuditEvent.builder()
                .correlationId("corr-123")
                .actor("ADMIN_USER")
                .action("ACCOUNT_STATUS_CHANGE")
                .resourceType("ACCOUNT")
                .resourceId("ACC100200")
                .status("SUCCESS")
                .build();

        when(auditLogRepository.findFirstByCorrelationIdAndActionAndResourceTypeAndResourceId(
                "corr-123", "ACCOUNT_STATUS_CHANGE", "ACCOUNT", "ACC100200"))
                .thenReturn(java.util.Optional.of(sampleLog));

        AuditLog result = auditService.recordAuditEvent(event);

        assertNotNull(result);
        assertEquals(sampleLog.getId(), result.getId());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should detect duplicate transaction event and skip saving")
    void recordTransactionEvent_Duplicate_ReturnsExistingWithoutSaving() {
        TransactionEvent txEvent = TransactionEvent.builder()
                .eventId("EVT-1")
                .correlationId("corr-tx")
                .transactionReference("TXN-998877")
                .customerId(50L)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .transactionType("TRANSFER")
                .status("SUCCESS")
                .build();

        when(auditLogRepository.findFirstByActionAndResourceTypeAndResourceIdAndStatus(
                "FINANCIAL_TRANSACTION_TRANSFER", "TRANSACTION", "TXN-998877", "SUCCESS"))
                .thenReturn(java.util.Optional.of(sampleLog));

        AuditLog result = auditService.recordTransactionEvent(txEvent);

        assertNotNull(result);
        assertEquals(sampleLog.getId(), result.getId());
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should sanitize sensitive fields in audit metadata")
    void recordAuditEvent_SanitizesSensitiveData() {
        AuditEvent event = AuditEvent.builder()
                .correlationId("corr-sensitive")
                .actor("CUSTOMER-1")
                .action("AUTH_LOGIN")
                .resourceType("USER")
                .resourceId("USR-1")
                .status("SUCCESS")
                .metadata("{\"password\":\"superSecret123\",\"token\":\"jwt-token-xyz\"}")
                .build();

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditService.recordAuditEvent(event);

        assertNotNull(saved);
        assertFalse(saved.getMetadata().contains("superSecret123"));
        assertFalse(saved.getMetadata().contains("jwt-token-xyz"));
        assertTrue(saved.getMetadata().contains("***REDACTED***"));
    }

    @Test
    @DisplayName("Should retrieve resource audit history")
    void getResourceHistory_Success() {
        when(auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc("ACCOUNT", "ACC100200"))
                .thenReturn(List.of(sampleLog));

        List<AuditLogResponseDto> history = auditService.getResourceHistory("ACCOUNT", "ACC100200");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("ACCOUNT_STATUS_CHANGE", history.get(0).getAction());
    }
}
