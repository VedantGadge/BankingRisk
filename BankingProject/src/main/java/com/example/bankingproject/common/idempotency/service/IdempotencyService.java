package com.example.bankingproject.common.idempotency.service;

import com.example.bankingproject.common.idempotency.exception.DuplicateRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed idempotency service.
 *
 * WHY Redis instead of PostgreSQL?
 * ─────────────────────────────────
 * Idempotency keys are short-lived key-value data (24h TTL) — exactly what
 * Redis is designed for. Advantages over the previous PostgreSQL approach:
 *
 *   1. Native TTL  — keys auto-expire after 24h. No cleanup job needed.
 *   2. Atomic SETNX — the "claim" operation is a single atomic Redis command
 *                     (SET key value NX EX ttl). No DB transactions needed.
 *   3. Speed       — Redis reads/writes are microsecond-latency vs milliseconds
 *                     for a DB round-trip with a unique constraint check.
 *   4. No lock     — REQUIRES_NEW JPA propagation is no longer needed.
 *
 * Redis Key Schema:
 *   idempotency:{clientKey}:status   → PROCESSING | COMPLETED | FAILED
 *   idempotency:{clientKey}:response → JSON-serialized response body
 *
 * Fail-open behaviour:
 *   If Redis is unavailable, we allow the request through rather than crashing
 *   the API. This matches banking best practice: prefer availability over strict
 *   idempotency during a Redis outage, and rely on DB-level constraints as a
 *   fallback safety net.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";

    /**
     * Returns the cached response for a COMPLETED, non-expired idempotency key.
     * Returns empty if the key is missing, expired (auto-handled by Redis TTL),
     * or in PROCESSING / FAILED state.
     */
    public <T> Optional<T> getCachedResponse(String key, Class<T> responseType) {
        try {
            String status = redisTemplate.opsForValue().get(statusKey(key));
            if (!COMPLETED.equals(status)) {
                return Optional.empty();
            }

            String json = redisTemplate.opsForValue().get(responseKey(key));
            if (json == null) return Optional.empty();

            return Optional.ofNullable(deserialize(json, responseType));

        } catch (RedisConnectionFailureException e) {
            log.warn("[idempotency] Redis unavailable, skipping cache lookup for key={}", key);
            return Optional.empty();
        }
    }

    /**
     * Atomically claims an idempotency key using Redis SETNX
     * (SET key value NX EX ttl — set only if the key does NOT already exist).
     *
     * If two threads race with the same key, exactly one wins the SETNX.
     * The loser sees the key already exists and throws DuplicateRequestException.
     *
     * If a previous attempt FAILED, the old key is deleted and retried.
     *
     * @throws DuplicateRequestException if a concurrent request is already PROCESSING.
     */
    public void claim(String key, String operationType) {
        try {
            // SETNX: atomic SET only if Not eXists — the core of the concurrency guarantee
            Boolean claimed = redisTemplate.opsForValue()
                    .setIfAbsent(statusKey(key), PROCESSING, TTL);

            if (Boolean.FALSE.equals(claimed)) {
                // Key already exists — inspect its current status
                String existingStatus = redisTemplate.opsForValue().get(statusKey(key));

                if (PROCESSING.equals(existingStatus)) {
                    throw new DuplicateRequestException(
                            "A request with this idempotency key is already being processed");
                }

                if (FAILED.equals(existingStatus)) {
                    // Previous attempt failed — delete old keys and allow retry
                    redisTemplate.delete(statusKey(key));
                    redisTemplate.delete(responseKey(key));
                    redisTemplate.opsForValue().set(statusKey(key), PROCESSING, TTL);
                    log.info("[idempotency] retrying previously failed key={}", key);
                }
                // If COMPLETED, getCachedResponse() should have been called first
                // and returned the cached result — this code path is unexpected.
            }

            log.info("[idempotency] claimed key={} operation={}", key, operationType);

        } catch (DuplicateRequestException e) {
            throw e; // rethrow — don't swallow
        } catch (RedisConnectionFailureException e) {
            log.warn("[idempotency] Redis unavailable, bypassing claim for key={}", key);
        }
    }

    /**
     * Marks the key as COMPLETED and caches the JSON-serialized response.
     * The same TTL is refreshed so the 24h window restarts from completion time.
     */
    public <T> void complete(String key, T response) {
        try {
            redisTemplate.opsForValue().set(responseKey(key), serialize(response), TTL);
            redisTemplate.opsForValue().set(statusKey(key), COMPLETED, TTL);
            log.info("[idempotency] completed key={}", key);
        } catch (RedisConnectionFailureException e) {
            log.warn("[idempotency] Redis unavailable, could not cache response for key={}", key);
        }
    }

    /**
     * Marks the key as FAILED so the client can safely retry with the same key.
     */
    public void fail(String key) {
        try {
            redisTemplate.opsForValue().set(statusKey(key), FAILED, TTL);
            log.warn("[idempotency] marked as failed key={}", key);
        } catch (RedisConnectionFailureException e) {
            log.warn("[idempotency] Redis unavailable, could not record failure for key={}", key);
        }
    }

    // ─────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────

    private String statusKey(String key) {
        return PREFIX + key + ":status";
    }

    private String responseKey(String key) {
        return PREFIX + key + ":response";
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
