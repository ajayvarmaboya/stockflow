package com.stockflow.inventory.api;

import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryDto {
    private final UUID productId;
    private final int quantity;
    private final long version;
    private final LocalDateTime updatedAt;

    public InventoryDto(UUID productId, int quantity, long version, LocalDateTime updatedAt) {
        this.productId = productId;
        this.quantity = quantity;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public long getVersion() { return version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
