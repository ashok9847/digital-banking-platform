package com.banking.transaction.repository;

import com.banking.transaction.entity.OutboxEvent;
import com.banking.transaction.entity.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Step 2E: Save and retrieve an OutboxEvent")
    void saveAndRetrieveOutboxEvent() {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent event = new OutboxEvent(
                eventId,
                "TRANSACTION",
                "TXN-100200",
                "TRANSACTION_COMPLETED",
                "bank.transaction.completed",
                "{\"amount\":100.0,\"currency\":\"USD\"}"
        );

        OutboxEvent saved = outboxEventRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getAggregateType()).isEqualTo("TRANSACTION");
        assertThat(saved.getAggregateId()).isEqualTo("TXN-100200");
        assertThat(saved.getEventType()).isEqualTo("TRANSACTION_COMPLETED");
        assertThat(saved.getTopic()).isEqualTo("bank.transaction.completed");
        assertThat(saved.getPayload()).contains("100.0");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPublishedAt()).isNull();

        Optional<OutboxEvent> found = outboxEventRepository.findByEventId(eventId);
        assertThat(found).isPresent();
        assertThat(found.get().getAggregateId()).isEqualTo("TXN-100200");
    }

    @Test
    @DisplayName("Step 2E: eventId uniqueness is enforced at database level")
    void eventIdUniquenessEnforced() {
        String sharedEventId = UUID.randomUUID().toString();
        OutboxEvent event1 = new OutboxEvent(
                sharedEventId,
                "TRANSACTION",
                "TXN-100",
                "TRANSACTION_COMPLETED",
                "bank.transaction.completed",
                "{\"amount\":100.0}"
        );
        outboxEventRepository.saveAndFlush(event1);

        OutboxEvent event2 = new OutboxEvent(
                sharedEventId,
                "TRANSACTION",
                "TXN-200",
                "TRANSACTION_COMPLETED",
                "bank.transaction.completed",
                "{\"amount\":200.0}"
        );

        assertThatThrownBy(() -> outboxEventRepository.saveAndFlush(event2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Step 2E: Default status is PENDING and retryCount is 0")
    void defaultStatusAndRetryCount() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "TRANSACTION",
                "TXN-DEFAULT",
                "TRANSACTION_COMPLETED",
                "bank.transaction.completed",
                "{}"
        );

        OutboxEvent saved = outboxEventRepository.save(event);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    @DisplayName("Step 2E: Polling query retrieves PENDING events in FIFO order")
    void findTop50ByStatusOrderByCreatedAtAsc() throws InterruptedException {
        OutboxEvent e1 = new OutboxEvent(
                UUID.randomUUID().toString(), "TRANSACTION", "TXN-1",
                "TRANSACTION_COMPLETED", "bank.transaction.completed", "{}"
        );
        outboxEventRepository.save(e1);
        Thread.sleep(10);

        OutboxEvent e2 = new OutboxEvent(
                UUID.randomUUID().toString(), "TRANSACTION", "TXN-2",
                "TRANSACTION_COMPLETED", "bank.transaction.completed", "{}"
        );
        outboxEventRepository.save(e2);
        Thread.sleep(10);

        OutboxEvent e3 = new OutboxEvent(
                UUID.randomUUID().toString(), "TRANSACTION", "TXN-3",
                "TRANSACTION_COMPLETED", "bank.transaction.completed", "{}"
        );
        e3.setStatus(OutboxStatus.PUBLISHED);
        outboxEventRepository.save(e3);

        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        assertThat(pending).hasSize(2);
        assertThat(pending.get(0).getAggregateId()).isEqualTo("TXN-1");
        assertThat(pending.get(1).getAggregateId()).isEqualTo("TXN-2");
    }

    @Test
    @DisplayName("Step 2E: Update status to PUBLISHED with publishedAt timestamp")
    void updateStatusToPublished() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "TRANSACTION",
                "TXN-PUB",
                "TRANSACTION_COMPLETED",
                "bank.transaction.completed",
                "{}"
        );
        OutboxEvent saved = outboxEventRepository.save(event);

        saved.setStatus(OutboxStatus.PUBLISHED);
        saved.setPublishedAt(Instant.now());
        OutboxEvent updated = outboxEventRepository.save(saved);

        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Step 5: findPendingForUpdate retrieves PENDING events in FIFO order under pessimistic write lock")
    void findPendingForUpdate_RetrievesPendingEventsInFifoOrder() throws InterruptedException {
        OutboxEvent e1 = new OutboxEvent(
                UUID.randomUUID().toString(), "TRANSACTION", "TXN-PESSIMISTIC-1",
                "TRANSACTION_COMPLETED", "bank.transaction.completed", "{}"
        );
        outboxEventRepository.save(e1);
        Thread.sleep(10);

        OutboxEvent e2 = new OutboxEvent(
                UUID.randomUUID().toString(), "TRANSACTION", "TXN-PESSIMISTIC-2",
                "TRANSACTION_COMPLETED", "bank.transaction.completed", "{}"
        );
        outboxEventRepository.save(e2);

        OutboxEvent e3 = new OutboxEvent(
                UUID.randomUUID().toString(), "TRANSACTION", "TXN-PESSIMISTIC-3",
                "TRANSACTION_COMPLETED", "bank.transaction.completed", "{}"
        );
        e3.setStatus(OutboxStatus.PUBLISHED);
        outboxEventRepository.save(e3);

        List<OutboxEvent> lockedPending = outboxEventRepository.findPendingForUpdate(
                OutboxStatus.PENDING, org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(lockedPending).hasSize(2);
        assertThat(lockedPending.get(0).getAggregateId()).isEqualTo("TXN-PESSIMISTIC-1");
        assertThat(lockedPending.get(1).getAggregateId()).isEqualTo("TXN-PESSIMISTIC-2");
    }
}
