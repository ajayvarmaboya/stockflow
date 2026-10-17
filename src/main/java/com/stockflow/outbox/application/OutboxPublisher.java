package com.stockflow.outbox.application;

import com.stockflow.outbox.domain.OutboxEvent;
import com.stockflow.outbox.domain.OutboxStatus;
import com.stockflow.outbox.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;

    public OutboxPublisher(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, 50)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Simulate publishing to message broker / internal bus
                log.info("Dispatched outbox event [{}]: type={}, aggregateId={}",
                        event.getId(), event.getEventType(), event.getAggregateId());

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                event.setAttempts(event.getAttempts() + 1);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: " + event.getId(), e);
                event.setAttempts(event.getAttempts() + 1);
                if (event.getAttempts() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
        }

        repository.saveAll(pendingEvents);
    }
}
