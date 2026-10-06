package com.banking.transaction.service;

import com.banking.common.event.TransactionEvent;
import com.banking.common.exception.InsufficientBalanceException;
import com.banking.common.exception.InvalidTransactionException;
import com.banking.transaction.client.AccountClient;
import com.banking.transaction.client.BalanceResultDto;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.dto.TransactionResponseDto;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.WithdrawRequest;
import com.banking.transaction.entity.OutboxEvent;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.repository.OutboxEventRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionFailureScenariosTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(idempotencyService.getCachedResponse(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Should reject deposit with zero or negative amount")
    void deposit_ZeroOrNegativeAmount_ThrowsInvalidTransactionException() {
        DepositRequest zeroReq = new DepositRequest("ACC100", BigDecimal.ZERO, "USD", "Zero");
        DepositRequest negReq = new DepositRequest("ACC100", new BigDecimal("-25.00"), "USD", "Negative");

        assertThrows(InvalidTransactionException.class, () -> transactionService.deposit(zeroReq, "k1"));
        assertThrows(InvalidTransactionException.class, () -> transactionService.deposit(negReq, "k2"));

        verify(accountClient, never()).credit(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject withdrawal exceeding single transaction limit of 10,000")
    void withdraw_ExceedingLimit_ThrowsInvalidTransactionException() {
        WithdrawRequest request = new WithdrawRequest("ACC100", new BigDecimal("15000.00"), "USD", "Large amount");

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                transactionService.withdraw(request, "k3"));

        assertTrue(ex.getMessage().contains("exceeds single transaction limit"));
        verify(accountClient, never()).debit(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject transfer between identical source and destination accounts")
    void transfer_SameAccount_ThrowsInvalidTransactionException() {
        TransferRequest request = new TransferRequest("ACC100", "acc100", new BigDecimal("50.00"), "USD", "Self transfer");

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                transactionService.transfer(request, "k4"));

        assertTrue(ex.getMessage().contains("Source and destination accounts cannot be the same"));
        verify(idempotencyService).fail("k4");
        verify(accountClient, never()).debit(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle insufficient balance on transfer and record FAILED transaction state")
    void transfer_InsufficientBalance_FailsAndPublishesFailedEvent() {
        TransferRequest request = new TransferRequest("ACC100", "ACC200", new BigDecimal("2000.00"), "USD", "Overdraft transfer");

        when(accountClient.debit(eq("ACC100"), eq(new BigDecimal("2000.00")), any()))
                .thenThrow(new InsufficientBalanceException("ACC100", new BigDecimal("100.00"), new BigDecimal("2000.00")));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(request, "k5"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(txCaptor.capture());

        Transaction lastSaved = txCaptor.getAllValues().get(txCaptor.getAllValues().size() - 1);
        assertEquals(TransactionStatus.FAILED, lastSaved.getStatus());
        assertNotNull(lastSaved.getFailureReason());

        verify(idempotencyService).fail("k5");
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(accountClient, never()).credit(eq("ACC200"), any(), any());
    }

    @Test
    @DisplayName("Should execute automatic compensation (Saga rollback) if credit leg fails during transfer")
    void transfer_CreditLegFails_ExecutesCompensatingCredit() {
        TransferRequest request = new TransferRequest("ACC100", "ACC200", new BigDecimal("150.00"), "USD", "Transfer with credit failure");

        when(accountClient.debit(eq("ACC100"), eq(new BigDecimal("150.00")), any()))
                .thenReturn(new BalanceResultDto("ACC100", new BigDecimal("500.00"), new BigDecimal("350.00"), "REF", "SUCCESS"));

        when(accountClient.credit(eq("ACC200"), eq(new BigDecimal("150.00")), any()))
                .thenThrow(new RuntimeException("Destination account blocked"));

        assertThrows(RuntimeException.class, () -> transactionService.transfer(request, "k6"));

        // Verify that compensating credit was issued back to source account
        verify(accountClient).credit(eq("ACC100"), eq(new BigDecimal("150.00")), contains("-COMPENSATE"));
        verify(idempotencyService).fail("k6");
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should successfully complete deposit and atomically persist OutboxEvent")
    void deposit_Success_PersistsOutboxEvent() {
        DepositRequest request = new DepositRequest("ACC100", new BigDecimal("200.00"), "USD", "Deposit with Outbox");

        when(accountClient.credit(eq("ACC100"), eq(new BigDecimal("200.00")), any()))
                .thenReturn(new BalanceResultDto("ACC100", new BigDecimal("1000.00"), new BigDecimal("1200.00"), "REF", "SUCCESS"));

        TransactionResponseDto response = transactionService.deposit(request, "k7");

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        verify(accountClient).credit(eq("ACC100"), eq(new BigDecimal("200.00")), any());
        verify(idempotencyService).complete(eq("k7"), any(), eq(201));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException when source account does not exist")
    void transfer_AccountNotFound_FailsAndRecordsFailedState() {
        TransferRequest request = new TransferRequest("ACC_UNKNOWN", "ACC200", new BigDecimal("50.00"), "USD", "Transfer unknown");

        when(accountClient.debit(eq("ACC_UNKNOWN"), any(), any()))
                .thenThrow(new com.banking.common.exception.ResourceNotFoundException("Account", "accountNumber", "ACC_UNKNOWN"));

        assertThrows(com.banking.common.exception.ResourceNotFoundException.class, () ->
                transactionService.transfer(request, "k8"));

        verify(idempotencyService).fail("k8");
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(accountClient, never()).credit(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle InvalidTransactionException when debiting inactive account")
    void transfer_InactiveAccount_FailsAndRecordsFailedState() {
        TransferRequest request = new TransferRequest("ACC_INACTIVE", "ACC200", new BigDecimal("50.00"), "USD", "Transfer inactive");

        when(accountClient.debit(eq("ACC_INACTIVE"), any(), any()))
                .thenThrow(new InvalidTransactionException("Cannot debit inactive account ACC_INACTIVE"));

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.transfer(request, "k9"));

        verify(idempotencyService).fail("k9");
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(accountClient, never()).credit(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle unexpected database failure and mark idempotency record as failed")
    void transfer_DatabaseFailure_FailsGracefully() {
        TransferRequest request = new TransferRequest("ACC100", "ACC200", new BigDecimal("50.00"), "USD", "Transfer DB error");

        when(accountClient.debit(eq("ACC100"), any(), any()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Database connection timeout"));

        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class, () ->
                transactionService.transfer(request, "k10"));

        verify(idempotencyService).fail("k10");
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
