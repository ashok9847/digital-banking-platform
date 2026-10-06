package com.banking.transaction.service;

import com.banking.common.exception.DuplicateResourceException;
import com.banking.transaction.entity.IdempotencyRecord;
import com.banking.transaction.entity.IdempotencyStatus;
import com.banking.transaction.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> responseType) {
        return getCachedResponse(idempotencyKey, null, responseType);
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> getCachedResponse(String idempotencyKey, Object requestPayload, Class<T> responseType) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();

        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new DuplicateResourceException("Transaction with idempotency key '" + idempotencyKey +
                    "' is currently processing. Please wait.");
        }

        // Validate payload hash to prevent key reuse with conflicting payload
        if (requestPayload != null && record.getRequestHash() != null && !"UNKNOWN".equals(record.getRequestHash())) {
            String incomingHash = computeHash(requestPayload);
            if (!record.getRequestHash().equals(incomingHash)) {
                throw new DuplicateResourceException("Idempotency key '" + idempotencyKey +
                        "' was previously used with a different request payload.");
            }
        }

        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponseBody() != null) {
            try {
                return Optional.of(objectMapper.readValue(record.getResponseBody(), responseType));
            } catch (JsonProcessingException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startProcessing(String idempotencyKey, String resourceType, Object requestPayload) {
        String hash = computeHash(requestPayload);
        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, resourceType, hash, IdempotencyStatus.IN_PROGRESS);
        try {
            idempotencyRepository.save(record);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Concurrent transaction with idempotency key '" + idempotencyKey +
                    "' is already processing.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String idempotencyKey, Object responsePayload, int statusCode) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            try {
                record.setResponseBody(objectMapper.writeValueAsString(responsePayload));
            } catch (JsonProcessingException e) {
                record.setResponseBody("{}");
            }
            record.setStatusCode(statusCode);
            record.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyRepository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String idempotencyKey) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            idempotencyRepository.save(record);
        });
    }

    private String computeHash(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            return "UNKNOWN";
        }
    }
}
