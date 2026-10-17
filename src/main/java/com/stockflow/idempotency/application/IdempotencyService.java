package com.stockflow.idempotency.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockflow.common.exception.IdempotencyConflictException;
import com.stockflow.idempotency.domain.IdempotencyRecord;
import com.stockflow.idempotency.persistence.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String computeHash(Object requestPayload) {
        try {
            String json = objectMapper.writeValueAsString(requestPayload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("Failed to compute request payload hash", e);
            throw new RuntimeException("Error computing idempotency hash", e);
        }
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> findExistingResult(String scope, String idempotencyKey, String currentHash, Class<T> responseClass) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        Optional<IdempotencyRecord> recordOpt = repository.findByScopeAndIdempotencyKey(scope, idempotencyKey);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();
        if (!record.getRequestHash().equals(currentHash)) {
            log.warn("Idempotency conflict: key '{}' in scope '{}' was previously used with a different request payload",
                    idempotencyKey, scope);
            throw new IdempotencyConflictException(
                    "Idempotency key '" + idempotencyKey + "' has already been used with a different request payload"
            );
        }

        try {
            T cachedResponse = objectMapper.readValue(record.getResponseBody(), responseClass);
            log.info("Returning cached response for idempotency key '{}' in scope '{}'", idempotencyKey, scope);
            return Optional.of(cachedResponse);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached idempotency response", e);
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSuccess(String scope, String idempotencyKey, String requestHash, int statusCode, Object response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = new IdempotencyRecord(
                    UUID.randomUUID(),
                    scope,
                    idempotencyKey,
                    requestHash,
                    statusCode,
                    jsonResponse,
                    LocalDateTime.now().plusDays(1)
            );
            repository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency storage", e);
        }
    }
}
