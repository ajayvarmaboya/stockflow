package com.stockflow.order;

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private UUID productId;

    @BeforeEach
    void setUp() {
        // Setup a product with strictly 1 available stock unit (Handbook 06 Concurrency Test Recipe)
        Product product = new Product(
                UUID.randomUUID(),
                "SKU-CONCURRENCY-" + UUID.randomUUID().toString().substring(0, 8),
                "Limited Flash Sale Item",
                "High demand item with exactly 1 unit available",
                new BigDecimal("99.99"),
                true
        );
        product = productRepository.save(product);
        productId = product.getId();

        Inventory inventory = new Inventory(productId, 1);
        inventoryRepository.save(inventory);
    }

    @Test
    @DisplayName("Concurrency Recipe: Two simultaneous buyers compete for the last single unit; exactly one succeeds and stock remains 0")
    void twoConcurrentBuyers_CompeteForLastUnit_ExactlyOneSucceeds() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final String customerId = "customer-concurrent-" + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // Block until all threads are ready, then fire at the exact same instant
                    startLatch.await();

                    CreateOrderRequest request = new CreateOrderRequest(
                            List.of(new OrderItemRequest(productId, 1))
                    );
                    OrderDto order = orderService.placeOrder(customerId, request, null);
                    if (order != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire simultaneous requests
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Handbook 06 Invariants:
        // 1. Exactly one order succeeds
        assertEquals(1, successCount.get(), "Expected exactly 1 order to succeed");
        // 2. Exactly one order fails with insufficient stock conflict
        assertEquals(1, failureCount.get(), "Expected exactly 1 order to fail with conflict");

        // 3. Inventory quantity is strictly 0, never negative
        Inventory finalInventory = inventoryRepository.findById(productId).orElseThrow();
        assertEquals(0, finalInventory.getQuantity(), "Inventory stock must be 0, never negative");
    }
}
