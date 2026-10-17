package com.stockflow.idempotency;

import com.stockflow.common.exception.IdempotencyConflictException;
import com.stockflow.inventory.domain.Inventory;
import com.stockflow.inventory.persistence.InventoryRepository;
import com.stockflow.order.api.CreateOrderRequest;
import com.stockflow.order.api.OrderDto;
import com.stockflow.order.api.OrderItemRequest;
import com.stockflow.order.application.OrderService;
import com.stockflow.product.domain.Product;
import com.stockflow.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdempotencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private UUID productAId;
    private UUID productBId;
    private String customerId;

    @BeforeEach
    void setUp() {
        customerId = "customer-idem-" + UUID.randomUUID();

        Product productA = productRepository.save(new Product(
                UUID.randomUUID(), "SKU-IDEM-A-" + UUID.randomUUID().toString().substring(0, 6),
                "Product A", "Description A", new BigDecimal("25.00"), true
        ));
        productAId = productA.getId();
        inventoryRepository.save(new Inventory(productAId, 10));

        Product productB = productRepository.save(new Product(
                UUID.randomUUID(), "SKU-IDEM-B-" + UUID.randomUUID().toString().substring(0, 6),
                "Product B", "Description B", new BigDecimal("15.00"), true
        ));
        productBId = productB.getId();
        inventoryRepository.save(new Inventory(productBId, 10));
    }

    @Test
    @DisplayName("Same Idempotency-Key + same payload returns cached logical order without double stock decrement")
    void sameKeyAndSamePayload_ReturnsCachedOrder() {
        String key = "key-test-123";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(productAId, 2))
        );

        // Initial submission
        OrderDto order1 = orderService.placeOrder(customerId, request, key);
        assertNotNull(order1);
        assertEquals(new BigDecimal("50.00"), order1.getTotalAmount());

        // Stock decreased from 10 to 8
        Inventory invAfterFirst = inventoryRepository.findById(productAId).orElseThrow();
        assertEquals(8, invAfterFirst.getQuantity());

        // Network retry with exact same key and payload
        OrderDto order2 = orderService.placeOrder(customerId, request, key);
        assertNotNull(order2);
        assertEquals(order1.getId(), order2.getId(), "Should return the identical logical order ID");
        assertEquals(order1.getTotalAmount(), order2.getTotalAmount());

        // Stock must still be 8, not decremented twice!
        Inventory invAfterRetry = inventoryRepository.findById(productAId).orElseThrow();
        assertEquals(8, invAfterRetry.getQuantity(), "Stock must not be decremented again on retry");
    }

    @Test
    @DisplayName("Same Idempotency-Key + different payload throws IdempotencyConflictException (409)")
    void sameKeyAndDifferentPayload_ThrowsIdempotencyConflict() {
        String key = "key-conflict-456";

        CreateOrderRequest request1 = new CreateOrderRequest(
                List.of(new OrderItemRequest(productAId, 1))
        );
        OrderDto order1 = orderService.placeOrder(customerId, request1, key);
        assertNotNull(order1);

        // Client attempts to reuse the same idempotency key for a different payload (different product/quantity)
        CreateOrderRequest request2 = new CreateOrderRequest(
                List.of(new OrderItemRequest(productBId, 3))
        );

        assertThrows(IdempotencyConflictException.class, () ->
                orderService.placeOrder(customerId, request2, key));
    }
}
