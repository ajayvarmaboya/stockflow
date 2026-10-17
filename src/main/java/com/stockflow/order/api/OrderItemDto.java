package com.stockflow.order.api;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemDto {
    private final UUID id;
    private final UUID productId;
    private final String sku;
    private final String name;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineTotal;

    public OrderItemDto(UUID id, UUID productId, String sku, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
