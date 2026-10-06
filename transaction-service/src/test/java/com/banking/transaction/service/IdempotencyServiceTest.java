package com.banking.transaction.service;

import com.banking.common.exception.DuplicateResourceException;
import com.banking.transaction.dto.DepositRequest;
import com.banking.transaction.dto.TransactionResponseDto;
import com.banking.transaction.entity.IdempotencyRecord;
import com.banking.transaction.entity.IdempotencyStatus;
import com.banking.transaction.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotencyService idempotencyService;

    private DepositRequest sampleRequest;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        sampleRequest = new DepositRequest("ACC100", new BigDecimal("100.00"), "USD", "Test deposit");
        idempotencyKey = "idemp-key-test-123";
    }

    @Test
    @DisplayName("Should return empty when idempotency key not found")
    void getCachedResponse_KeyNotFound_ReturnsEmpty() {
        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        Optional<TransactionResponseDto> result = idempotencyService.getCachedResponse(
                idempotencyKey, sampleRequest, TransactionResponseDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when transaction is IN_PROGRESS")
    void getCachedResponse_InProgress_ThrowsException() {
        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, "DEPOSIT", "hash123", IdempotencyStatus.IN_PROGRESS);
        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                idempotencyService.getCachedResponse(idempotencyKey, sampleRequest, TransactionResponseDto.class));

        assertTrue(ex.getMessage().contains("currently processing"));
    }

    @Test
    @DisplayName("Should return cached response when completed with matching payload hash")
    void getCachedResponse_CompletedMatchingHash_ReturnsCachedDto() throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(sampleRequest);
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        String hash = java.util.HexFormat.of().formatHex(digest.digest(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, "DEPOSIT", hash, IdempotencyStatus.COMPLETED);
        record.setResponseBody("{\"transactionReference\":\"TXN-123\",\"status\":\"SUCCESS\",\"amount\":100.00}");
        record.setStatusCode(201);

        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        Optional<TransactionResponseDto> result = idempotencyService.getCachedResponse(
                idempotencyKey, sampleRequest, TransactionResponseDto.class);

        assertTrue(result.isPresent());
        assertEquals("TXN-123", result.get().getTransactionReference());
        assertEquals("SUCCESS", result.get().getStatus());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when key is reused with different request payload")
    void getCachedResponse_PayloadMismatch_ThrowsException() {
        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, "DEPOSIT", "different_hash_abc", IdempotencyStatus.COMPLETED);
        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                idempotencyService.getCachedResponse(idempotencyKey, sampleRequest, TransactionResponseDto.class));

        assertTrue(ex.getMessage().contains("different request payload"));
    }

    @Test
    @DisplayName("Should handle concurrent race condition on startProcessing and throw DuplicateResourceException")
    void startProcessing_ConcurrentRace_ThrowsDuplicateResourceException() {
        when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'idempotency_key'"));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                idempotencyService.startProcessing(idempotencyKey, "DEPOSIT", sampleRequest));

        assertTrue(ex.getMessage().contains("already processing"));
    }

    @Test
    @DisplayName("Should mark idempotency record as COMPLETED with response payload")
    void complete_SavesCompletedRecord() {
        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, "DEPOSIT", "hash123", IdempotencyStatus.IN_PROGRESS);
        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        TransactionResponseDto response = TransactionResponseDto.builder()
                .transactionReference("TXN-999")
                .status("SUCCESS")
                .build();

        idempotencyService.complete(idempotencyKey, response, 201);

        assertEquals(IdempotencyStatus.COMPLETED, record.getStatus());
        assertEquals(201, record.getStatusCode());
        assertNotNull(record.getResponseBody());
        assertTrue(record.getResponseBody().contains("TXN-999"));
        verify(idempotencyRepository).save(record);
    }

    @Test
    @DisplayName("Should mark idempotency record as FAILED")
    void fail_SavesFailedRecord() {
        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, "DEPOSIT", "hash123", IdempotencyStatus.IN_PROGRESS);
        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(record));

        idempotencyService.fail(idempotencyKey);

        assertEquals(IdempotencyStatus.FAILED, record.getStatus());
        verify(idempotencyRepository).save(record);
    }
}
