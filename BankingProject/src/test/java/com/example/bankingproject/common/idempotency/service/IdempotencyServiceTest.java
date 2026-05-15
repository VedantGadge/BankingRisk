package com.example.bankingproject.common.idempotency.service;

import com.example.bankingproject.common.idempotency.exception.DuplicateRequestException;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private IdempotencyService idempotencyService;

    private static final String KEY = "test-idempotency-key-uuid";
    private static final String STATUS_KEY = "idempotency:" + KEY + ":status";
    private static final String RESPONSE_KEY = "idempotency:" + KEY + ":response";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        idempotencyService = new IdempotencyService(redisTemplate, objectMapper);
    }

    // ─────────────────────────────────────────────────
    // getCachedResponse
    // ─────────────────────────────────────────────────

    @Test
    void getCachedResponse_shouldReturnCachedResultForCompletedKey() {
        // Pre-built JSON matching TransactionResponse fields
        String json = """
                {
                  "id": 1,
                  "fromUserId": 1,
                  "toUserId": 2,
                  "amount": 500.00,
                  "type": "TRANSFER",
                  "status": "COMPLETED",
                  "createdAt": "2026-05-15T12:00:00"
                }
                """;

        when(valueOps.get(STATUS_KEY)).thenReturn("COMPLETED");
        when(valueOps.get(RESPONSE_KEY)).thenReturn(json);

        Optional<TransactionResponse> result = idempotencyService.getCachedResponse(
                KEY, TransactionResponse.class);

        assertTrue(result.isPresent());
        assertEquals(TransactionStatus.COMPLETED, result.get().getStatus());
        assertEquals(0, result.get().getAmount().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void getCachedResponse_shouldReturnEmptyForProcessingKey() {
        when(valueOps.get(STATUS_KEY)).thenReturn("PROCESSING");

        Optional<TransactionResponse> result = idempotencyService.getCachedResponse(
                KEY, TransactionResponse.class);

        assertTrue(result.isEmpty());
        // Should not even try to read the response key if status != COMPLETED
        verify(valueOps, never()).get(RESPONSE_KEY);
    }

    @Test
    void getCachedResponse_shouldReturnEmptyWhenKeyNotFound() {
        when(valueOps.get(STATUS_KEY)).thenReturn(null);

        Optional<TransactionResponse> result = idempotencyService.getCachedResponse(
                KEY, TransactionResponse.class);

        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────────
    // claim
    // ─────────────────────────────────────────────────

    @Test
    void claim_shouldSucceedForNewKey() {
        // SETNX returns true → key did not exist → successfully claimed
        when(valueOps.setIfAbsent(eq(STATUS_KEY), eq("PROCESSING"), any()))
                .thenReturn(true);

        assertDoesNotThrow(() -> idempotencyService.claim(KEY, "TRANSFER"));

        verify(valueOps, times(1)).setIfAbsent(eq(STATUS_KEY), eq("PROCESSING"), any());
    }

    @Test
    void claim_shouldThrowDuplicateRequestExceptionWhenAlreadyProcessing() {
        // SETNX returns false → key already exists
        when(valueOps.setIfAbsent(eq(STATUS_KEY), eq("PROCESSING"), any()))
                .thenReturn(false);
        // Existing status is PROCESSING (concurrent request)
        when(valueOps.get(STATUS_KEY)).thenReturn("PROCESSING");

        assertThrows(DuplicateRequestException.class,
                () -> idempotencyService.claim(KEY, "TRANSFER"));
    }

    @Test
    void claim_shouldDeleteAndRetryForFailedKey() {
        // SETNX returns false → key already exists
        when(valueOps.setIfAbsent(eq(STATUS_KEY), eq("PROCESSING"), any()))
                .thenReturn(false);
        // Existing status is FAILED → allow retry
        when(valueOps.get(STATUS_KEY)).thenReturn("FAILED");

        assertDoesNotThrow(() -> idempotencyService.claim(KEY, "TRANSFER"));

        // Both keys must be deleted before re-claiming
        verify(redisTemplate, times(1)).delete(STATUS_KEY);
        verify(redisTemplate, times(1)).delete(RESPONSE_KEY);
        // New PROCESSING record must be set
        verify(valueOps, times(1)).set(eq(STATUS_KEY), eq("PROCESSING"), any());
    }

    // ─────────────────────────────────────────────────
    // complete & fail
    // ─────────────────────────────────────────────────

    @Test
    void complete_shouldStoreResponseAndSetCompletedStatus() {
        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .fromUserId(1L)
                .toUserId(2L)
                .amount(new BigDecimal("100"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();

        assertDoesNotThrow(() -> idempotencyService.complete(KEY, response));

        // Response JSON must be stored
        verify(valueOps, times(1)).set(eq(RESPONSE_KEY), anyString(), any());
        // Status must be updated to COMPLETED
        verify(valueOps, times(1)).set(eq(STATUS_KEY), eq("COMPLETED"), any());
    }

    @Test
    void fail_shouldSetFailedStatus() {
        assertDoesNotThrow(() -> idempotencyService.fail(KEY));

        verify(valueOps, times(1)).set(eq(STATUS_KEY), eq("FAILED"), any());
    }
}
