package com.banking.transaction.publisher;

import com.banking.common.event.EventTopics;
import com.banking.common.event.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private TransactionEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new TransactionEventPublisherImpl(kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish completed transaction event with transaction reference as partition key")
    void testPublishTransactionCompleted_Success() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("EVT-100")
                .transactionReference("TXN-REF-100")
                .sourceAccountNumber("ACC-SRC")
                .destinationAccountNumber("ACC-DST")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .transactionType("TRANSFER")
                .status("SUCCESS")
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_COMPLETED), eq("TXN-REF-100"), eq(event)))
                .thenReturn(future);

        publisher.publishTransactionCompleted(event);

        verify(kafkaTemplate, times(1)).send(EventTopics.TRANSACTION_COMPLETED, "TXN-REF-100", event);
    }

    @Test
    @DisplayName("Should publish failed transaction event with transaction reference as partition key")
    void testPublishTransactionFailed_Success() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("EVT-200")
                .transactionReference("TXN-REF-200")
                .sourceAccountNumber("ACC-SRC")
                .amount(new BigDecimal("5000.00"))
                .currency("USD")
                .transactionType("WITHDRAWAL")
                .status("FAILED")
                .failureReason("Insufficient balance")
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_FAILED), eq("TXN-REF-200"), eq(event)))
                .thenReturn(future);

        publisher.publishTransactionFailed(event);

        verify(kafkaTemplate, times(1)).send(EventTopics.TRANSACTION_FAILED, "TXN-REF-200", event);
    }

    @Test
    @DisplayName("Should publish created transaction event with transaction reference as partition key")
    void testPublishTransactionCreated_Success() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("EVT-300")
                .transactionReference("TXN-REF-300")
                .sourceAccountNumber("ACC-SRC")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .transactionType("DEPOSIT")
                .status("PENDING")
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(EventTopics.TRANSACTION_CREATED), eq("TXN-REF-300"), eq(event)))
                .thenReturn(future);

        publisher.publishTransactionCreated(event);

        verify(kafkaTemplate, times(1)).send(EventTopics.TRANSACTION_CREATED, "TXN-REF-300", event);
    }

    @Test
    @DisplayName("Should gracefully handle KafkaTemplate send exceptions without throwing to caller")
    void testPublishEvent_KafkaException_HandledGracefully() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("EVT-400")
                .transactionReference("TXN-REF-400")
                .build();

        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka broker unreachable"));

        assertThatCode(() -> publisher.publishTransactionCompleted(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should resolve transactionReference as key when present")
    void testResolvePartitionKey() {
        TransactionEvent event = TransactionEvent.builder()
                .transactionReference("TXN-KEY-1")
                .sourceAccountNumber("ACC-1")
                .destinationAccountNumber("ACC-2")
                .build();

        assertThat(publisher.resolvePartitionKey(event)).isEqualTo("TXN-KEY-1");
    }
}
