package com.banking.transaction.config;

import com.banking.common.event.EventTopics;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name(EventTopics.TRANSACTION_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCompletedTopic() {
        return TopicBuilder.name(EventTopics.TRANSACTION_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionFailedTopic() {
        return TopicBuilder.name(EventTopics.TRANSACTION_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRequestedTopic() {
        return TopicBuilder.name(EventTopics.NOTIFICATION_REQUESTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditCreatedTopic() {
        return TopicBuilder.name(EventTopics.AUDIT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCompletedDltTopic() {
        return TopicBuilder.name(EventTopics.TRANSACTION_COMPLETED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCreatedDltTopic() {
        return TopicBuilder.name(EventTopics.TRANSACTION_CREATED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionFailedDltTopic() {
        return TopicBuilder.name(EventTopics.TRANSACTION_FAILED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRequestedDltTopic() {
        return TopicBuilder.name(EventTopics.NOTIFICATION_REQUESTED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditCreatedDltTopic() {
        return TopicBuilder.name(EventTopics.AUDIT_CREATED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
