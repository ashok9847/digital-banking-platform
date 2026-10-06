package com.banking.transaction.publisher;

import com.banking.common.event.TransactionEvent;
import com.banking.transaction.entity.OutboxEvent;
import com.banking.transaction.entity.OutboxStatus;
import com.banking.transaction.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.publisher.enabled:true}")
    private boolean enabled;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.publisher.send-timeout-seconds:5}")
    private int sendTimeoutSeconds;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    @Transactional
    public void schedulePublishPendingEvents() {
        if (!enabled) {
            return;
        }
        publishPendingEvents();
    }

    @Transactional
    public int publishPendingEvents() {
        if (!enabled) {
            log.debug("Outbox publisher is disabled, skipping polling");
            return 0;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingForUpdate(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize)
        );

        if (pendingEvents.isEmpty()) {
            return 0;
        }

        log.info("Outbox publisher picked {} pending event(s)", pendingEvents.size());
        int publishedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            if (publishSingleEvent(event)) {
                publishedCount++;
            }
        }

        return publishedCount;
    }

    private boolean publishSingleEvent(OutboxEvent event) {
        String partitionKey = (event.getAggregateId() != null && !event.getAggregateId().isBlank())
                ? event.getAggregateId()
                : event.getEventId();

        try {
            log.info("Attempting Kafka publish for outbox event: id={}, eventId={}, txRef={}, topic={}, retryCount={}",
                    event.getId(), event.getEventId(), event.getAggregateId(), event.getTopic(), event.getRetryCount());

            TransactionEvent transactionEvent = objectMapper.readValue(event.getPayload(), TransactionEvent.class);

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    event.getTopic(),
                    partitionKey,
                    transactionEvent
            );

            SendResult<String, Object> sendResult = future.get(sendTimeoutSeconds, TimeUnit.SECONDS);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            outboxEventRepository.save(event);

            log.info("Kafka publish acknowledged, marked outbox event PUBLISHED: id={}, eventId={}, txRef={}, partition={}, offset={}",
                    event.getId(), event.getEventId(), event.getAggregateId(),
                    sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset());
            return true;
        } catch (Exception ex) {
            int newRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(newRetryCount);

            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            if (errorMsg.length() > 490) {
                errorMsg = errorMsg.substring(0, 490);
            }
            event.setLastError(errorMsg);

            if (newRetryCount >= maxRetries) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event reached max retries ({}), marked FAILED: id={}, eventId={}, txRef={}, error={}",
                        maxRetries, event.getId(), event.getEventId(), event.getAggregateId(), errorMsg);
            } else {
                log.warn("Kafka publish failed, will retry: id={}, eventId={}, txRef={}, retryCount={}/{}, error={}",
                        event.getId(), event.getEventId(), event.getAggregateId(), newRetryCount, maxRetries, errorMsg);
            }

            outboxEventRepository.save(event);
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getSendTimeoutSeconds() {
        return sendTimeoutSeconds;
    }

    public void setSendTimeoutSeconds(int sendTimeoutSeconds) {
        this.sendTimeoutSeconds = sendTimeoutSeconds;
    }
}
