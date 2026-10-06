package com.banking.transaction.publisher;

import com.banking.common.event.EventTopics;
import com.banking.common.event.TransactionEvent;
import com.banking.transaction.entity.OutboxEvent;
import com.banking.transaction.entity.OutboxStatus;
import com.banking.transaction.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(outboxPublisher, "enabled", true);
        ReflectionTestUtils.setField(outboxPublisher, "batchSize", 50);
        ReflectionTestUtils.setField(outboxPublisher, "maxRetries", 5);
        ReflectionTestUtils.setField(outboxPublisher, "sendTimeoutSeconds", 2);
    }

    private OutboxEvent createSampleEvent(Long id, String eventId, String txRef, int retryCount) throws Exception {
        TransactionEvent txEvent = TransactionEvent.builder()
                .eventId(eventId)
                .transactionReference(txRef)
                .sourceAccountNumber("ACC100")
                .destinationAccountNumber("ACC200")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .transactionType("TRANSFER")
                .status("SUCCESS")
                .timestamp(Instant.now())
                .build();

        String payload = objectMapper.writeValueAsString(txEvent);
        OutboxEvent event = new OutboxEvent(
                eventId,
                "TRANSACTION",
                txRef,
                "TRANSACTION_COMPLETED",
                EventTopics.TRANSACTION_COMPLETED,
                payload
        );
        event.setId(id);
        event.setRetryCount(retryCount);
        return event;
    }

    @Test
    @DisplayName("Should retrieve pending events, publish to Kafka, and mark status as PUBLISHED upon acknowledgement")
    void publishPendingEvents_Success_MarksPublished() throws Exception {
        OutboxEvent event = createSampleEvent(1L, "evt-1", "TXN-001", 0);
        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(EventTopics.TRANSACTION_COMPLETED, 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_COMPLETED), eq("TXN-001"), any(TransactionEvent.class)))
                .thenReturn(future);

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertNull(event.getLastError());
        verify(kafkaTemplate, times(1)).send(eq(EventTopics.TRANSACTION_COMPLETED), eq("TXN-001"), any(TransactionEvent.class));
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("Should increment retry count and set lastError when Kafka publish encounters failure, without marking PUBLISHED")
    void publishPendingEvents_KafkaFailure_IncrementsRetryCount() throws Exception {
        OutboxEvent event = createSampleEvent(2L, "evt-2", "TXN-002", 0);
        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_COMPLETED), eq("TXN-002"), any(TransactionEvent.class)))
                .thenReturn(failedFuture);

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(0, publishedCount);
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getLastError());
        assertTrue(event.getLastError().contains("Kafka broker unavailable"));
        assertNull(event.getPublishedAt());
        verify(outboxEventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("Should mark event as FAILED when max retries are reached, without deleting the row")
    void publishPendingEvents_MaxRetriesReached_MarksFailed() throws Exception {
        OutboxEvent event = createSampleEvent(3L, "evt-3", "TXN-003", 4);
        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Permanent network partition"));

        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_COMPLETED), eq("TXN-003"), any(TransactionEvent.class)))
                .thenReturn(failedFuture);

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(0, publishedCount);
        assertEquals(5, event.getRetryCount());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertNotNull(event.getLastError());
        verify(outboxEventRepository, times(1)).save(event);
        verify(outboxEventRepository, never()).delete(any());
        verify(outboxEventRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should skip polling entirely when publisher is disabled")
    void publishPendingEvents_Disabled_SkipsProcessing() {
        outboxPublisher.setEnabled(false);

        int count = outboxPublisher.publishPendingEvents();

        assertEquals(0, count);
        verify(outboxEventRepository, never()).findPendingForUpdate(any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 0 and do nothing when no pending outbox events exist")
    void publishPendingEvents_NoPendingEvents_ReturnsZero() {
        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of());

        int count = outboxPublisher.publishPendingEvents();

        assertEquals(0, count);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should process multiple pending events in FIFO order and publish each to Kafka")
    void publishPendingEvents_MultipleEvents_ProcessedInOrder() throws Exception {
        OutboxEvent event1 = createSampleEvent(10L, "evt-10", "TXN-010", 0);
        OutboxEvent event2 = createSampleEvent(11L, "evt-11", "TXN-011", 0);
        OutboxEvent event3 = createSampleEvent(12L, "evt-12", "TXN-012", 0);

        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event1, event2, event3));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(EventTopics.TRANSACTION_COMPLETED, 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(3, publishedCount);
        assertEquals(OutboxStatus.PUBLISHED, event1.getStatus());
        assertEquals(OutboxStatus.PUBLISHED, event2.getStatus());
        assertEquals(OutboxStatus.PUBLISHED, event3.getStatus());
        verify(kafkaTemplate, times(3)).send(any(), any(), any());
        verify(outboxEventRepository, times(3)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should record error and increment retry count when Kafka send times out")
    void publishPendingEvents_KafkaTimeout_HandledGracefully() throws Exception {
        OutboxEvent event = createSampleEvent(4L, "evt-4", "TXN-004", 1);
        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> timeoutFuture = new CompletableFuture<>() {
            @Override
            public SendResult<String, Object> get(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException, java.util.concurrent.ExecutionException, TimeoutException {
                throw new TimeoutException("Kafka acknowledgment timeout after 2 seconds");
            }
        };

        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_COMPLETED), eq("TXN-004"), any(TransactionEvent.class)))
                .thenReturn(timeoutFuture);

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(0, publishedCount);
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(2, event.getRetryCount());
        assertTrue(event.getLastError().contains("timeout"));
        assertNull(event.getPublishedAt());
    }

    @Test
    @DisplayName("Should use eventId as partition key if aggregateId is null or empty")
    void publishPendingEvents_NullAggregateId_UsesEventIdAsPartitionKey() throws Exception {
        OutboxEvent event = createSampleEvent(5L, "evt-5", null, 0);
        event.setAggregateId(null);

        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(EventTopics.TRANSACTION_COMPLETED, 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_COMPLETED), eq("evt-5"), any(TransactionEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        verify(kafkaTemplate).send(eq(EventTopics.TRANSACTION_COMPLETED), eq("evt-5"), any(TransactionEvent.class));
    }

    @Test
    @DisplayName("Should handle corrupted payload by recording error without crashing publisher loop")
    void publishPendingEvents_CorruptedPayload_HandlesAndIncrementsRetry() {
        OutboxEvent corruptedEvent = new OutboxEvent(
                "corrupted-evt",
                "TRANSACTION",
                "TXN-CORRUPT",
                "TRANSACTION_COMPLETED",
                EventTopics.TRANSACTION_COMPLETED,
                "NOT_VALID_JSON{{{"
        );
        corruptedEvent.setId(6L);

        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(corruptedEvent));

        int publishedCount = outboxPublisher.publishPendingEvents();

        assertEquals(0, publishedCount);
        assertEquals(1, corruptedEvent.getRetryCount());
        assertNotNull(corruptedEvent.getLastError());
        assertEquals(OutboxStatus.PENDING, corruptedEvent.getStatus());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("schedulePublishPendingEvents triggers publish when enabled")
    void schedulePublishPendingEvents_TriggersPublish() throws Exception {
        OutboxEvent event = createSampleEvent(7L, "evt-7", "TXN-007", 0);
        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(EventTopics.TRANSACTION_COMPLETED, 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, metadata)));

        outboxPublisher.schedulePublishPendingEvents();

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        verify(outboxEventRepository).findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should prevent concurrent double-publication when competing threads poll")
    void publishPendingEvents_ConcurrentExecutions_SafeAgainstDoublePublishing() throws Exception {
        OutboxEvent event = createSampleEvent(100L, "evt-concurrent", "TXN-CONC", 0);

        when(outboxEventRepository.findPendingForUpdate(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event))
                .thenReturn(List.of());

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(EventTopics.TRANSACTION_COMPLETED, 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, metadata)));

        int count1 = outboxPublisher.publishPendingEvents();
        int count2 = outboxPublisher.publishPendingEvents();

        assertEquals(1, count1);
        assertEquals(0, count2);
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        verify(kafkaTemplate, times(1)).send(any(), any(), any());
    }
}
