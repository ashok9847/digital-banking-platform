package com.banking.transaction.service;

import com.banking.common.event.EventTopics;
import com.banking.common.event.TransactionEvent;
import com.banking.common.exception.InsufficientBalanceException;
import com.banking.transaction.client.AccountClient;
import com.banking.transaction.client.BalanceResultDto;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.dto.TransactionResponseDto;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.WithdrawRequest;
import com.banking.transaction.entity.OutboxEvent;
import com.banking.transaction.entity.OutboxStatus;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.repository.IdempotencyRecordRepository;
import com.banking.transaction.repository.OutboxEventRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TransactionOutboxAtomicityIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountClient accountClient;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("Step 3F-1: Successful transfer creates Transaction record and OutboxEvent atomically")
    void transfer_Success_CreatesTransactionAndOutboxAtomically() throws Exception {
        TransferRequest request = new TransferRequest(
                "ACC-SRC-101", "ACC-DST-202", new BigDecimal("250.00"), "USD", "Rent transfer"
        );
        String idempotencyKey = "idemp-transfer-outbox-001";

        when(accountClient.debit(eq("ACC-SRC-101"), eq(new BigDecimal("250.00")), any()))
                .thenReturn(new BalanceResultDto("ACC-SRC-101", new BigDecimal("1000.00"), new BigDecimal("750.00"), "REF-1", "SUCCESS"));
        when(accountClient.credit(eq("ACC-DST-202"), eq(new BigDecimal("250.00")), any()))
                .thenReturn(new BalanceResultDto("ACC-DST-202", new BigDecimal("500.00"), new BigDecimal("750.00"), "REF-2", "SUCCESS"));

        TransactionResponseDto response = transactionService.transfer(request, idempotencyKey);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        // Verify Transaction persisted
        Optional<Transaction> txOpt = transactionRepository.findByTransactionReference(response.getTransactionReference());
        assertThat(txOpt).isPresent();
        Transaction tx = txOpt.get();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getAmount()).isEqualByComparingTo("250.00");

        // Verify OutboxEvent persisted atomically
        List<OutboxEvent> outboxList = outboxEventRepository.findByAggregateTypeAndAggregateId("TRANSACTION", tx.getTransactionReference());
        assertThat(outboxList).hasSize(1);
        OutboxEvent outbox = outboxList.get(0);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getEventType()).isEqualTo("TRANSACTION_COMPLETED");
        assertThat(outbox.getTopic()).isEqualTo(EventTopics.TRANSACTION_COMPLETED);
        assertThat(outbox.getRetryCount()).isEqualTo(0);
        assertThat(outbox.getPublishedAt()).isNull();

        // Verify payload JSON correlation
        TransactionEvent payloadEvent = objectMapper.readValue(outbox.getPayload(), TransactionEvent.class);
        assertThat(payloadEvent.getEventId()).isEqualTo(outbox.getEventId());
        assertThat(payloadEvent.getTransactionReference()).isEqualTo(tx.getTransactionReference());
        assertThat(payloadEvent.getAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Step 3F-2: Successful deposit creates Transaction record and OutboxEvent atomically")
    void deposit_Success_CreatesTransactionAndOutboxAtomically() throws Exception {
        DepositRequest request = new DepositRequest("ACC-DEP-100", new BigDecimal("150.00"), "USD", "Deposit cash");
        String idempotencyKey = "idemp-dep-outbox-001";

        when(accountClient.credit(eq("ACC-DEP-100"), eq(new BigDecimal("150.00")), any()))
                .thenReturn(new BalanceResultDto("ACC-DEP-100", new BigDecimal("200.00"), new BigDecimal("350.00"), "REF-DEP", "SUCCESS"));

        TransactionResponseDto response = transactionService.deposit(request, idempotencyKey);

        Optional<Transaction> txOpt = transactionRepository.findByTransactionReference(response.getTransactionReference());
        assertThat(txOpt).isPresent();

        List<OutboxEvent> outboxList = outboxEventRepository.findByAggregateTypeAndAggregateId("TRANSACTION", response.getTransactionReference());
        assertThat(outboxList).hasSize(1);
        OutboxEvent outbox = outboxList.get(0);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getTopic()).isEqualTo(EventTopics.TRANSACTION_COMPLETED);

        TransactionEvent event = objectMapper.readValue(outbox.getPayload(), TransactionEvent.class);
        assertThat(event.getTransactionReference()).isEqualTo(response.getTransactionReference());
    }

    @Test
    @DisplayName("Step 3F-3: Successful withdrawal creates Transaction record and OutboxEvent atomically")
    void withdraw_Success_CreatesTransactionAndOutboxAtomically() throws Exception {
        WithdrawRequest request = new WithdrawRequest("ACC-WTH-100", new BigDecimal("75.00"), "USD", "ATM withdrawal");
        String idempotencyKey = "idemp-wth-outbox-001";

        when(accountClient.debit(eq("ACC-WTH-100"), eq(new BigDecimal("75.00")), any()))
                .thenReturn(new BalanceResultDto("ACC-WTH-100", new BigDecimal("500.00"), new BigDecimal("425.00"), "REF-WTH", "SUCCESS"));

        TransactionResponseDto response = transactionService.withdraw(request, idempotencyKey);

        Optional<Transaction> txOpt = transactionRepository.findByTransactionReference(response.getTransactionReference());
        assertThat(txOpt).isPresent();

        List<OutboxEvent> outboxList = outboxEventRepository.findByAggregateTypeAndAggregateId("TRANSACTION", response.getTransactionReference());
        assertThat(outboxList).hasSize(1);
        OutboxEvent outbox = outboxList.get(0);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getTopic()).isEqualTo(EventTopics.TRANSACTION_COMPLETED);
    }

    @Test
    @DisplayName("Step 3F-4: Business validation failure prevents any Transaction or OutboxEvent creation")
    void transfer_SameSourceAndDest_RollsBackEverything() {
        TransferRequest request = new TransferRequest("ACC-SAME", "ACC-SAME", new BigDecimal("100.00"), "USD", "Invalid");
        String idempotencyKey = "idemp-invalid-same";

        assertThatThrownBy(() -> transactionService.transfer(request, idempotencyKey))
                .isInstanceOf(Exception.class);

        assertThat(transactionRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Step 3F-5: Account debit failure causes atomic rollback of both Transaction and OutboxEvent")
    void transfer_DebitFails_RollsBackTransactionAndOutboxAtomically() {
        TransferRequest request = new TransferRequest("ACC-SRC", "ACC-DST", new BigDecimal("100.00"), "USD", "Transfer");
        String idempotencyKey = "idemp-debit-fail";

        when(accountClient.debit(any(), any(), any()))
                .thenThrow(new InsufficientBalanceException("Insufficient funds"));

        assertThatThrownBy(() -> transactionService.transfer(request, idempotencyKey))
                .isInstanceOf(InsufficientBalanceException.class);

        // Verify ATOMIC ROLLBACK: Nothing is partially committed
        assertThat(transactionRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Step 3F-6: Idempotent repeat request does not duplicate OutboxEvent")
    void transfer_RepeatIdempotentKey_ReturnsCachedResponseWithoutNewOutboxEvent() {
        TransferRequest request = new TransferRequest("ACC-SRC-REPEAT", "ACC-DST-REPEAT", new BigDecimal("50.00"), "USD", "Transfer");
        String idempotencyKey = "idemp-repeat-outbox-key";

        when(accountClient.debit(any(), any(), any()))
                .thenReturn(new BalanceResultDto("ACC-SRC-REPEAT", new BigDecimal("200.00"), new BigDecimal("150.00"), "REF", "SUCCESS"));
        when(accountClient.credit(any(), any(), any()))
                .thenReturn(new BalanceResultDto("ACC-DST-REPEAT", new BigDecimal("100.00"), new BigDecimal("150.00"), "REF", "SUCCESS"));

        TransactionResponseDto first = transactionService.transfer(request, idempotencyKey);
        assertThat(first.getStatus()).isEqualTo("SUCCESS");

        // Repeat identical request
        TransactionResponseDto second = transactionService.transfer(request, idempotencyKey);
        assertThat(second.getTransactionReference()).isEqualTo(first.getTransactionReference());

        // Verify exactly one transaction and one outbox event exist
        assertThat(transactionRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }
}
