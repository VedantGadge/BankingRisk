package com.example.bankingproject.common.idempotency.service;

import com.example.bankingproject.common.idempotency.entity.IdempotencyRecord;
import com.example.bankingproject.common.idempotency.enums.IdempotencyStatus;
import com.example.bankingproject.common.idempotency.exception.DuplicateRequestException;
import com.example.bankingproject.common.idempotency.repository.IdempotencyRecordRepository;
import com.example.bankingproject.transaction.dto.TransactionResponse;
import com.example.bankingproject.transaction.enums.TransactionStatus;
import com.example.bankingproject.transaction.enums.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    private IdempotencyService idempotencyService;

    private static final String KEY = "test-idempotency-key-uuid";

    @BeforeEach
    void setUp() {
        // Manually wire so we control ObjectMapper configuration.
        // @Spy + @InjectMocks doesn't handle field injection correctly
        // when the constructor also takes the ObjectMapper.
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        idempotencyService = new IdempotencyService(repository, objectMapper);
    }

    // ─────────────────────────────────────────────────
    // getCachedResponse
    // ─────────────────────────────────────────────────

    @Test
    void getCachedResponse_shouldReturnCachedResultForCompletedKey() throws Exception {
        // Build a simple JSON payload that matches TransactionResponse fields.
        // We use a raw string so we avoid Jackson/Lombok @Builder constructor issues.
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

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(KEY)
                .status(IdempotencyStatus.COMPLETED)
                .responsePayload(json)
                .expiresAt(LocalDateTime.now().plusHours(23))
                .build();

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(record));

        Optional<TransactionResponse> result = idempotencyService.getCachedResponse(
                KEY, TransactionResponse.class);

        assertTrue(result.isPresent());
        assertEquals(TransactionStatus.COMPLETED, result.get().getStatus());
        assertEquals(0, result.get().getAmount().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void getCachedResponse_shouldReturnEmptyForExpiredKey() {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(KEY)
                .status(IdempotencyStatus.COMPLETED)
                .responsePayload("{}")
                // Expired yesterday
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(record));

        Optional<TransactionResponse> result = idempotencyService.getCachedResponse(
                KEY, TransactionResponse.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCachedResponse_shouldReturnEmptyWhenKeyNotFound() {
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        Optional<TransactionResponse> result = idempotencyService.getCachedResponse(
                KEY, TransactionResponse.class);

        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────────
    // claim
    // ─────────────────────────────────────────────────

    @Test
    void claim_shouldSaveProcessingRecordForNewKey() {
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        idempotencyService.claim(KEY, "TRANSFER");

        verify(repository, times(1)).save(argThat(record ->
                record.getIdempotencyKey().equals(KEY) &&
                record.getStatus() == IdempotencyStatus.PROCESSING &&
                record.getOperationType().equals("TRANSFER")
        ));
    }

    @Test
    void claim_shouldThrowDuplicateRequestExceptionWhenAlreadyProcessing() {
        IdempotencyRecord processingRecord = IdempotencyRecord.builder()
                .idempotencyKey(KEY)
                .status(IdempotencyStatus.PROCESSING)
                .build();

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(processingRecord));

        assertThrows(DuplicateRequestException.class,
                () -> idempotencyService.claim(KEY, "TRANSFER"));

        // Existing record must not be overwritten
        verify(repository, never()).save(any());
    }

    @Test
    void claim_shouldDeleteFailedRecordAndAllowRetry() {
        IdempotencyRecord failedRecord = IdempotencyRecord.builder()
                .idempotencyKey(KEY)
                .status(IdempotencyStatus.FAILED)
                .build();

        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(failedRecord));

        idempotencyService.claim(KEY, "TRANSFER");

        // Old FAILED record must be deleted before creating new PROCESSING record
        verify(repository, times(1)).delete(failedRecord);
        verify(repository, times(1)).flush();
        verify(repository, times(1)).save(any(IdempotencyRecord.class));
    }
}
