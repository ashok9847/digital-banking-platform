package com.banking.notification.consumer;

import com.banking.common.event.EventTopics;
import com.banking.common.event.NotificationEvent;
import com.banking.common.event.TransactionEvent;
import com.banking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);
    private final NotificationService notificationService;

    public NotificationKafkaConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = EventTopics.TRANSACTION_COMPLETED, groupId = "${spring.kafka.consumer.group-id:banking-notification-group}")
    public void consumeTransactionCompleted(TransactionEvent event) {
        log.info("Kafka consumer received [{}] for txRef: {}",
                EventTopics.TRANSACTION_COMPLETED, event.getTransactionReference());
        notificationService.processTransactionEvent(event);
    }

    @KafkaListener(topics = EventTopics.NOTIFICATION_REQUESTED, groupId = "${spring.kafka.consumer.group-id:banking-notification-group}")
    public void consumeNotificationRequested(NotificationEvent event) {
        log.info("Kafka consumer received [{}] for customerId: {}",
                EventTopics.NOTIFICATION_REQUESTED, event.getCustomerId());
        notificationService.processDirectNotification(event);
    }

    @KafkaListener(topics = EventTopics.TRANSACTION_FAILED, groupId = "${spring.kafka.consumer.group-id:banking-notification-group}")
    public void consumeTransactionFailed(TransactionEvent event) {
        log.info("Kafka consumer received [{}] for txRef: {}",
                EventTopics.TRANSACTION_FAILED, event.getTransactionReference());
        notificationService.processTransactionEvent(event);
    }
}
