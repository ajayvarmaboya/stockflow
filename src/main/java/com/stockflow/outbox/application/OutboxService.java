package com.stockflow.outbox.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockflow.outbox.domain.OutboxEvent;
import com.stockflow.outbox.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateType, String aggregateId, String eventType, Object data) {
        try {
            UUID eventId = UUID.randomUUID();
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("eventId", eventId.toString());
            envelope.put("eventType", eventType);
            envelope.put("occurredAt", Instant.now().toString());
            envelope.put("aggregateType", aggregateType);
            envelope.put("aggregateId", aggregateId);
            envelope.put("schemaVersion", 1);
            envelope.put("data", data);

            String payload = objectMapper.writeValueAsString(envelope);

            OutboxEvent event = new OutboxEvent(
                    eventId,
                    aggregateType,
                    aggregateId,
                    eventType,
                    payload
            );
            repository.save(event);
            log.debug("Recorded outbox event: id={}, type={}, aggregateId={}", eventId, eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Error serializing outbox event", e);
        }
    }
}
