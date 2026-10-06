package com.banking.audit.consumer;

import com.banking.audit.service.AuditService;
import com.banking.common.event.AuditEvent;
import com.banking.common.event.EventTopics;
import com.banking.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConsumer.class);
    private final AuditService auditService;

    public AuditKafkaConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(topics = EventTopics.AUDIT_CREATED, groupId = "${spring.kafka.consumer.group-id:banking-audit-group}")
    public void consumeAuditEvent(AuditEvent event) {
        log.info("Audit consumer received [{}] for action: {}", EventTopics.AUDIT_CREATED, event.getAction());
        auditService.recordAuditEvent(event);
    }

    @KafkaListener(topics = {EventTopics.TRANSACTION_CREATED, EventTopics.TRANSACTION_COMPLETED, EventTopics.TRANSACTION_FAILED}, groupId = "${spring.kafka.consumer.group-id:banking-audit-group}")
    public void consumeTransactionEvent(TransactionEvent event) {
        log.info("Audit consumer received transaction event for ref: {}, status: {}",
                event.getTransactionReference(), event.getStatus());
        auditService.recordTransactionEvent(event);
    }
}
