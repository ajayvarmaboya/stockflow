package com.stockflow.order.api;

import com.stockflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderDto {
    private final UUID id;
    private final String customerId;
    private final OrderStatus status;
    private final BigDecimal totalAmount;
    private final String currency;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<OrderItemDto> items;

    public OrderDto(UUID id, String customerId, OrderStatus status, BigDecimal totalAmount, String currency,
                    LocalDateTime createdAt, LocalDateTime updatedAt, List<OrderItemDto> items) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItemDto> getItems() { return items; }
}
