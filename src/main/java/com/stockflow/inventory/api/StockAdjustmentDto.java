package com.stockflow.inventory.api;

import java.time.LocalDateTime;
import java.util.UUID;

public class StockAdjustmentDto {
    private final UUID id;
    private final UUID productId;
    private final int delta;
    private final String reason;
    private final String actorId;
    private final LocalDateTime createdAt;

    public StockAdjustmentDto(UUID id, UUID productId, int delta, String reason, String actorId, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.delta = delta;
        this.reason = reason;
        this.actorId = actorId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public int getDelta() { return delta; }
    public String getReason() { return reason; }
    public String getActorId() { return actorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
