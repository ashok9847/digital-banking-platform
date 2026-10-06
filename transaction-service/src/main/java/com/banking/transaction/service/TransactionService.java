package com.banking.transaction.service;

import com.banking.common.event.EventTopics;
import com.banking.common.event.TransactionEvent;
import com.banking.common.exception.InvalidTransactionException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.common.filter.CorrelationIdFilter;
import com.banking.transaction.client.AccountClient;
import com.banking.transaction.dto.*;
import com.banking.transaction.entity.*;
import com.banking.transaction.repository.OutboxEventRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final BigDecimal MAX_SINGLE_TRANSACTION_LIMIT = new BigDecimal("10000.0000");

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final IdempotencyService idempotencyService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountClient accountClient,
                              IdempotencyService idempotencyService,
                              OutboxEventRepository outboxEventRepository,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
        this.idempotencyService = idempotencyService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponseDto deposit(DepositRequest request, String idempotencyKey) {
        // 1. Check idempotency
        Optional<TransactionResponseDto> cached = idempotencyService.getCachedResponse(idempotencyKey, request, TransactionResponseDto.class);
        if (cached.isPresent()) {
            log.info("Returning idempotent cached response for deposit key {}", idempotencyKey);
            return cached.get();
        }
        idempotencyService.startProcessing(idempotencyKey, "DEPOSIT", request);

        validateAmount(request.getAmount());

        String reference = generateTransactionReference();
        Transaction transaction = new Transaction(
                reference,
                idempotencyKey,
                null,
                request.getDestinationAccountNumber(),
                request.getAmount(),
                request.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING,
                request.getDescription()
        );
        transactionRepository.save(transaction);

        try {
            accountClient.credit(request.getDestinationAccountNumber(), request.getAmount(), reference);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponseDto response = mapToDto(saved);
            idempotencyService.complete(idempotencyKey, response, 201);

            publishEvent(saved, null);
            return response;
        } catch (Exception ex) {
            log.error("Deposit failed for destination {}: {}", request.getDestinationAccountNumber(), ex.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            transactionRepository.save(transaction);
            idempotencyService.fail(idempotencyKey);
            publishEvent(transaction, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public TransactionResponseDto withdraw(WithdrawRequest request, String idempotencyKey) {
        Optional<TransactionResponseDto> cached = idempotencyService.getCachedResponse(idempotencyKey, request, TransactionResponseDto.class);
        if (cached.isPresent()) {
            log.info("Returning idempotent cached response for withdraw key {}", idempotencyKey);
            return cached.get();
        }
        idempotencyService.startProcessing(idempotencyKey, "WITHDRAWAL", request);

        validateAmount(request.getAmount());
        validateTransactionLimit(request.getAmount());

        String reference = generateTransactionReference();
        Transaction transaction = new Transaction(
                reference,
                idempotencyKey,
                request.getSourceAccountNumber(),
                null,
                request.getAmount(),
                request.getCurrency(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.PENDING,
                request.getDescription()
        );
        transactionRepository.save(transaction);

        try {
            accountClient.debit(request.getSourceAccountNumber(), request.getAmount(), reference);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponseDto response = mapToDto(saved);
            idempotencyService.complete(idempotencyKey, response, 201);

            publishEvent(saved, null);
            return response;
        } catch (Exception ex) {
            log.error("Withdrawal failed for source {}: {}", request.getSourceAccountNumber(), ex.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            transactionRepository.save(transaction);
            idempotencyService.fail(idempotencyKey);
            publishEvent(transaction, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public TransactionResponseDto transfer(TransferRequest request, String idempotencyKey) {
        Optional<TransactionResponseDto> cached = idempotencyService.getCachedResponse(idempotencyKey, request, TransactionResponseDto.class);
        if (cached.isPresent()) {
            log.info("Returning idempotent cached response for transfer key {}", idempotencyKey);
            return cached.get();
        }
        idempotencyService.startProcessing(idempotencyKey, "TRANSFER", request);

        if (request.getSourceAccountNumber().equalsIgnoreCase(request.getDestinationAccountNumber())) {
            idempotencyService.fail(idempotencyKey);
            throw new InvalidTransactionException("Source and destination accounts cannot be the same");
        }

        validateAmount(request.getAmount());
        validateTransactionLimit(request.getAmount());

        String reference = generateTransactionReference();
        Transaction transaction = new Transaction(
                reference,
                idempotencyKey,
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber(),
                request.getAmount(),
                request.getCurrency(),
                TransactionType.TRANSFER,
                TransactionStatus.PENDING,
                request.getDescription()
        );
        transactionRepository.save(transaction);

        try {
            // Atomic Debit from source
            accountClient.debit(request.getSourceAccountNumber(), request.getAmount(), reference);

            // Atomic Credit to destination
            try {
                accountClient.credit(request.getDestinationAccountNumber(), request.getAmount(), reference);
            } catch (Exception creditEx) {
                log.error("Credit leg failed for destination {}. Initiating automatic compensation...", request.getDestinationAccountNumber());
                accountClient.credit(request.getSourceAccountNumber(), request.getAmount(), reference + "-COMPENSATE");
                throw creditEx;
            }

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(Instant.now());
            Transaction saved = transactionRepository.save(transaction);

            TransactionResponseDto response = mapToDto(saved);
            idempotencyService.complete(idempotencyKey, response, 201);

            publishEvent(saved, null);
            return response;
        } catch (Exception ex) {
            log.error("Transfer failed from {} to {}: {}", request.getSourceAccountNumber(), request.getDestinationAccountNumber(), ex.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            transactionRepository.save(transaction);
            idempotencyService.fail(idempotencyKey);
            publishEvent(transaction, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByReference(String reference) {
        Transaction tx = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "reference", reference));
        return mapToDto(tx);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByAccount(String accountNumber, Pageable pageable) {
        return transactionRepository.findByAccountNumber(accountNumber, pageable).map(this::mapToDto);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be strictly greater than 0");
        }
    }

    private void validateTransactionLimit(BigDecimal amount) {
        if (amount.compareTo(MAX_SINGLE_TRANSACTION_LIMIT) > 0) {
            throw new InvalidTransactionException("Transaction amount exceeds single transaction limit of " + MAX_SINGLE_TRANSACTION_LIMIT);
        }
    }

    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void publishEvent(Transaction tx, String failureReason) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        String eventId = UUID.randomUUID().toString();
        TransactionEvent event = TransactionEvent.builder()
                .eventId(eventId)
                .transactionReference(tx.getTransactionReference())
                .idempotencyKey(tx.getIdempotencyKey())
                .sourceAccountNumber(tx.getSourceAccountNumber())
                .destinationAccountNumber(tx.getDestinationAccountNumber())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .transactionType(tx.getTransactionType().name())
                .status(tx.getStatus().name())
                .failureReason(failureReason)
                .correlationId(correlationId != null ? correlationId : "N/A")
                .timestamp(Instant.now())
                .build();

        // Outbox Pattern: Atomic Outbox Event Persistence within current DB transaction.
        // Publication to Kafka is now exclusively handled by OutboxPublisher.
        saveOutboxEvent(tx, event);
    }

    private void saveOutboxEvent(Transaction tx, TransactionEvent event) {
        try {
            String topic = (tx.getStatus() == TransactionStatus.SUCCESS)
                    ? EventTopics.TRANSACTION_COMPLETED
                    : EventTopics.TRANSACTION_FAILED;
            String eventType = (tx.getStatus() == TransactionStatus.SUCCESS)
                    ? "TRANSACTION_COMPLETED"
                    : "TRANSACTION_FAILED";

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    event.getEventId(),
                    "TRANSACTION",
                    tx.getTransactionReference(),
                    eventType,
                    topic,
                    payload
            );

            outboxEventRepository.save(outboxEvent);
            log.info("Persisted OutboxEvent atomically: id={}, eventId={}, txRef={}, status={}",
                    outboxEvent.getId(), outboxEvent.getEventId(), tx.getTransactionReference(), outboxEvent.getStatus());
        } catch (Exception ex) {
            log.error("Failed to persist OutboxEvent for txRef [{}]: {}", tx.getTransactionReference(), ex.getMessage());
            throw new RuntimeException("Failed to persist outbox event", ex);
        }
    }

    private TransactionResponseDto mapToDto(Transaction tx) {
        return TransactionResponseDto.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .idempotencyKey(tx.getIdempotencyKey())
                .sourceAccountNumber(tx.getSourceAccountNumber())
                .destinationAccountNumber(tx.getDestinationAccountNumber())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .transactionType(tx.getTransactionType().name())
                .status(tx.getStatus().name())
                .description(tx.getDescription())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}
