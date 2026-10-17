package com.stockflow.product.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProductDto {
    private final UUID id;
    private final String sku;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final boolean active;
    private final Integer availableStock;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProductDto(UUID id, String sku, String name, String description, BigDecimal price, boolean active,
                      Integer availableStock, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = active;
        this.availableStock = availableStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public Integer getAvailableStock() { return availableStock; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
