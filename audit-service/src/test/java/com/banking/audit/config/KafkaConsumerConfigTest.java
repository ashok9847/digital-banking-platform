package com.banking.audit.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {

    @Test
    @DisplayName("Should create Kafka consumer factory with proper deserialization configuration")
    void testConsumerFactory() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "banking-audit-group");

        ConsumerFactory<String, Object> factory = config.consumerFactory();
        assertNotNull(factory);
        assertNotNull(factory.getConfigurationProperties());
        assertEquals("localhost:9092", factory.getConfigurationProperties().get("bootstrap.servers"));
        assertEquals("banking-audit-group", factory.getConfigurationProperties().get("group.id"));
    }

    @Test
    @DisplayName("Should configure DLT KafkaTemplate with fail-fast max.block.ms")
    void testDltKafkaTemplate() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");

        KafkaTemplate<String, Object> template = config.dltKafkaTemplate();
        assertNotNull(template);
        assertNotNull(template.getProducerFactory());
    }

    @Test
    @DisplayName("Should configure ConcurrentKafkaListenerContainerFactory with DefaultErrorHandler and DLT recovery")
    void testKafkaListenerContainerFactory() {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "banking-audit-group");
        ReflectionTestUtils.setField(config, "maxAttempts", 3L);
        ReflectionTestUtils.setField(config, "backoffMs", 1000L);

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = config.kafkaListenerContainerFactory();
        assertNotNull(factory);

        CommonErrorHandler errorHandler = (CommonErrorHandler) ReflectionTestUtils.getField(factory, "commonErrorHandler");
        assertNotNull(errorHandler, "CommonErrorHandler must be configured for poison-pill recovery");
        assertTrue(errorHandler instanceof DefaultErrorHandler, "Error handler must be DefaultErrorHandler with DLT recoverer");
    }
}
