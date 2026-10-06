package com.banking.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Should build and serialize TransactionCompletedEvent without sensitive data")
    void testTransactionCompletedEvent_Serialization() throws Exception {
        String eventId = UUID.randomUUID().toString();
        String txId = "TXN-100200300";
        Instant now = Instant.now();

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .eventId(eventId)
                .transactionId(txId)
                .transactionType("TRANSFER")
                .sourceAccount("ACC10001")
                .destinationAccount("ACC20002")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .customerId(42L)
                .timestamp(now)
                .build();

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getTransactionId()).isEqualTo(txId);
        assertThat(event.getSourceAccount()).isEqualTo("ACC10001");
        assertThat(event.getDestinationAccount()).isEqualTo("ACC20002");
        assertThat(event.getAmount()).isEqualByComparingTo("250.00");
        assertThat(event.getCurrency()).isEqualTo("USD");
        assertThat(event.getCustomerId()).isEqualTo(42L);

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains(txId);
        assertThat(json).contains("ACC10001");
        assertThat(json).contains("ACC20002");
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("secret");
    }

    @Test
    @DisplayName("Should maintain backward compatibility with TransactionEvent alias methods")
    void testTransactionEvent_Aliases() {
        TransactionEvent event = TransactionEvent.builder()
                .eventId("E-1")
                .transactionReference("TXN-REF-99")
                .sourceAccountNumber("SRC-1")
                .destinationAccountNumber("DST-2")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .transactionType("DEPOSIT")
                .status("SUCCESS")
                .build();

        assertThat(event.getTransactionId()).isEqualTo("TXN-REF-99");
        assertThat(event.getSourceAccount()).isEqualTo("SRC-1");
        assertThat(event.getDestinationAccount()).isEqualTo("DST-2");
    }
}
