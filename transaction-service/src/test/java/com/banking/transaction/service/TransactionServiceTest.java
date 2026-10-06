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
import com.banking.transaction.repository.OutboxEventRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class TransactionServiceTest {

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
        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should successfully deposit funds and publish completed event")
    void deposit_Success() {
        DepositRequest request = new DepositRequest("ACC100200", new BigDecimal("500.00"), "USD", "Paycheck");
        String idempotencyKey = "idemp-dep-001";

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class))).thenReturn(Optional.empty());
        when(accountClient.credit(eq("ACC100200"), eq(new BigDecimal("500.00")), any())).thenReturn(
                new BalanceResultDto("ACC100200", new BigDecimal("1000.00"), new BigDecimal("1500.00"), "REF", "SUCCESS")
        );

        TransactionResponseDto response = transactionService.deposit(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("500.00"), response.getAmount());
        assertEquals("ACC100200", response.getDestinationAccountNumber());
        verify(accountClient).credit(eq("ACC100200"), eq(new BigDecimal("500.00")), any());
        verify(idempotencyService).complete(eq(idempotencyKey), any(TransactionResponseDto.class), eq(201));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should return cached response on duplicate idempotency key without re-executing credit")
    void deposit_DuplicateIdempotencyKey_ReturnsCached() {
        DepositRequest request = new DepositRequest("ACC100200", new BigDecimal("500.00"), "USD", "Paycheck");
        String idempotencyKey = "idemp-dep-duplicate";

        TransactionResponseDto cachedDto = TransactionResponseDto.builder()
                .transactionReference("TXN-CACHED-001")
                .status("SUCCESS")
                .amount(new BigDecimal("500.00"))
                .build();

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class)))
                .thenReturn(Optional.of(cachedDto));

        TransactionResponseDto response = transactionService.deposit(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("TXN-CACHED-001", response.getTransactionReference());
        verify(accountClient, never()).credit(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully withdraw funds")
    void withdraw_Success() {
        WithdrawRequest request = new WithdrawRequest("ACC100200", new BigDecimal("200.00"), "USD", "ATM");
        String idempotencyKey = "idemp-wth-001";

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class))).thenReturn(Optional.empty());
        when(accountClient.debit(eq("ACC100200"), eq(new BigDecimal("200.00")), any())).thenReturn(
                new BalanceResultDto("ACC100200", new BigDecimal("1000.00"), new BigDecimal("800.00"), "REF", "SUCCESS")
        );

        TransactionResponseDto response = transactionService.withdraw(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        assertEquals("ACC100200", response.getSourceAccountNumber());
        verify(accountClient).debit(eq("ACC100200"), eq(new BigDecimal("200.00")), any());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should return cached response on duplicate withdraw idempotency key without re-executing debit")
    void withdraw_DuplicateIdempotencyKey_ReturnsCached() {
        WithdrawRequest request = new WithdrawRequest("ACC100200", new BigDecimal("200.00"), "USD", "ATM");
        String idempotencyKey = "idemp-wth-duplicate";

        TransactionResponseDto cachedDto = TransactionResponseDto.builder()
                .transactionReference("TXN-CACHED-WTH-001")
                .status("SUCCESS")
                .amount(new BigDecimal("200.00"))
                .build();

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class)))
                .thenReturn(Optional.of(cachedDto));

        TransactionResponseDto response = transactionService.withdraw(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("TXN-CACHED-WTH-001", response.getTransactionReference());
        verify(accountClient, never()).debit(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return cached response on duplicate transfer idempotency key without re-executing transfer")
    void transfer_DuplicateIdempotencyKey_ReturnsCached() {
        TransferRequest request = new TransferRequest("ACC100", "ACC200", new BigDecimal("150.00"), "USD", "Rent");
        String idempotencyKey = "idemp-trf-duplicate";

        TransactionResponseDto cachedDto = TransactionResponseDto.builder()
                .transactionReference("TXN-CACHED-TRF-001")
                .status("SUCCESS")
                .amount(new BigDecimal("150.00"))
                .build();

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class)))
                .thenReturn(Optional.of(cachedDto));

        TransactionResponseDto response = transactionService.transfer(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("TXN-CACHED-TRF-001", response.getTransactionReference());
        verify(accountClient, never()).debit(any(), any(), any());
        verify(accountClient, never()).credit(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully transfer funds between two distinct accounts")
    void transfer_Success() {
        TransferRequest request = new TransferRequest("ACC100", "ACC200", new BigDecimal("150.00"), "USD", "Rent");
        String idempotencyKey = "idemp-trf-001";

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class))).thenReturn(Optional.empty());
        when(accountClient.debit(eq("ACC100"), eq(new BigDecimal("150.00")), any())).thenReturn(
                new BalanceResultDto("ACC100", new BigDecimal("1000.00"), new BigDecimal("850.00"), "REF", "SUCCESS")
        );
        when(accountClient.credit(eq("ACC200"), eq(new BigDecimal("150.00")), any())).thenReturn(
                new BalanceResultDto("ACC200", new BigDecimal("500.00"), new BigDecimal("650.00"), "REF", "SUCCESS")
        );

        TransactionResponseDto response = transactionService.transfer(request, idempotencyKey);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("ACC100", response.getSourceAccountNumber());
        assertEquals("ACC200", response.getDestinationAccountNumber());
        verify(accountClient).debit(eq("ACC100"), eq(new BigDecimal("150.00")), any());
        verify(accountClient).credit(eq("ACC200"), eq(new BigDecimal("150.00")), any());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when source and destination are the same")
    void transfer_SameAccount_ThrowsException() {
        TransferRequest request = new TransferRequest("ACC100", "ACC100", new BigDecimal("50.00"), "USD", "Self transfer");
        String idempotencyKey = "idemp-trf-same";

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class))).thenReturn(Optional.empty());

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(request, idempotencyKey));
        verify(accountClient, never()).debit(any(), any(), any());
        verify(idempotencyService).fail(idempotencyKey);
    }

    @Test
    @DisplayName("Should handle insufficient funds during withdrawal and record failed outbox event")
    void withdraw_InsufficientBalance_HandlesFailure() {
        WithdrawRequest request = new WithdrawRequest("ACC100", new BigDecimal("5000.00"), "USD", "Large withdrawal");
        String idempotencyKey = "idemp-wth-insuf";

        when(idempotencyService.getCachedResponse(eq(idempotencyKey), any(), eq(TransactionResponseDto.class))).thenReturn(Optional.empty());
        when(accountClient.debit(eq("ACC100"), eq(new BigDecimal("5000.00")), any()))
                .thenThrow(new InsufficientBalanceException("ACC100", new BigDecimal("200.00"), new BigDecimal("5000.00")));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(request, idempotencyKey));
        verify(idempotencyService).fail(idempotencyKey);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
