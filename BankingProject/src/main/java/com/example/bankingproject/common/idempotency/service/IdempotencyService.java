package com.example.bankingproject.common.idempotency.service;

import com.example.bankingproject.common.idempotency.entity.IdempotencyRecord;
import com.example.bankingproject.common.idempotency.enums.IdempotencyStatus;
import com.example.bankingproject.common.idempotency.exception.DuplicateRequestException;
import com.example.bankingproject.common.idempotency.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Manages idempotency keys to prevent duplicate transaction processing.
 *
 * Uses REQUIRES_NEW propagation so idempotency state commits independently
 * of the outer business transaction — a failed transfer still records the
 * failure in the idempotency table, allowing safe retries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Returns the cached response for a completed, non-expired idempotency key.
     */
    public <T> Optional<T> getCachedResponse(String key, Class<T> responseType) {
        return repository.findByIdempotencyKey(key)
                .filter(r -> r.getStatus() == IdempotencyStatus.COMPLETED)
                .filter(r -> r.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(r -> deserialize(r.getResponsePayload(), responseType));
    }

    /**
     * Claims an idempotency key by inserting a PROCESSING record.
     * Uses the database unique constraint for atomicity — if two threads race,
     * one wins the INSERT and the other gets a DataIntegrityViolationException.
     *
     * If the key was previously FAILED, the old record is removed to allow retry.
     *
     * @throws DuplicateRequestException if the key is already PROCESSING
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claim(String key, String operationType) {
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new DuplicateRequestException(
                        "A request with this idempotency key is already being processed");
            }

            // Allow retry if the previous attempt failed
            if (record.getStatus() == IdempotencyStatus.FAILED) {
                repository.delete(record);
                repository.flush();
            }
        }

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key)
                .operationType(operationType)
                .status(IdempotencyStatus.PROCESSING)
                .build();

        try {
            repository.save(record);
            log.info("[idempotency] claimed key={} operation={}", key, operationType);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRequestException(
                    "A request with this idempotency key is already being processed");
        }
    }

    /**
     * Marks the key as COMPLETED and caches the serialized response.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> void complete(String key, T response) {
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponsePayload(serialize(response));
            record.setHttpStatus(200);
            repository.save(record);
            log.info("[idempotency] completed key={}", key);
        });
    }

    /**
     * Marks the key as FAILED so the client can safely retry with the same key.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String key) {
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            repository.save(record);
            log.warn("[idempotency] marked as failed key={}", key);
        });
    }

    private <T> String serialize(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[idempotency] serialization failed", e);
            throw new RuntimeException("Failed to serialize idempotency response", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("[idempotency] deserialization failed", e);
            throw new RuntimeException("Failed to deserialize cached response", e);
        }
    }
}
