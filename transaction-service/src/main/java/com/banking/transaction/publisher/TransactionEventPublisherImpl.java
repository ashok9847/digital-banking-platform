package com.banking.transaction.publisher;

import com.banking.common.event.EventTopics;
import com.banking.common.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisherImpl implements TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisherImpl.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.transaction-created:bank.transaction.created}")
    private String transactionCreatedTopic = EventTopics.TRANSACTION_CREATED;

    @Value("${kafka.topics.transaction-completed:bank.transaction.completed}")
    private String transactionCompletedTopic = EventTopics.TRANSACTION_COMPLETED;

    @Value("${kafka.topics.transaction-failed:bank.transaction.failed}")
    private String transactionFailedTopic = EventTopics.TRANSACTION_FAILED;

    public TransactionEventPublisherImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishTransactionCreated(TransactionEvent event) {
        String partitionKey = resolvePartitionKey(event);
        log.info("Dispatching transaction created event to Kafka topic [{}]: key={}, txRef={}",
                transactionCreatedTopic, partitionKey, event.getTransactionReference());

        sendEvent(transactionCreatedTopic, partitionKey, event, "created");
    }

    @Override
    public void publishTransactionCompleted(TransactionEvent event) {
        String partitionKey = resolvePartitionKey(event);
        log.info("Dispatching transaction completed event to Kafka topic [{}]: key={}, txRef={}",
                transactionCompletedTopic, partitionKey, event.getTransactionReference());

        sendEvent(transactionCompletedTopic, partitionKey, event, "completed");
    }

    @Override
    public void publishTransactionFailed(TransactionEvent event) {
        String partitionKey = resolvePartitionKey(event);
        log.warn("Dispatching transaction failed event to Kafka topic [{}]: key={}, txRef={}",
                transactionFailedTopic, partitionKey, event.getTransactionReference());

        sendEvent(transactionFailedTopic, partitionKey, event, "failed");
    }

    private void sendEvent(String topic, String partitionKey, TransactionEvent event, String eventType) {
        try {
            kafkaTemplate.send(topic, partitionKey, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to deliver transaction {} event [{}]: {}",
                                    eventType, event.getTransactionReference(), ex.getMessage());
                        } else {
                            log.info("Successfully delivered transaction {} event [{}] to partition [{}] with offset [{}]",
                                    eventType,
                                    event.getTransactionReference(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("Error initiating Kafka send for {} transaction [{}]: {}",
                    eventType, event.getTransactionReference(), ex.getMessage());
        }
    }

    public String resolvePartitionKey(TransactionEvent event) {
        if (event.getTransactionReference() != null && !event.getTransactionReference().isBlank()) {
            return event.getTransactionReference();
        }
        if (event.getSourceAccountNumber() != null && !event.getSourceAccountNumber().isBlank()) {
            return event.getSourceAccountNumber();
        }
        if (event.getDestinationAccountNumber() != null && !event.getDestinationAccountNumber().isBlank()) {
            return event.getDestinationAccountNumber();
        }
        return event.getEventId();
    }
}
