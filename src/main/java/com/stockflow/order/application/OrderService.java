package com.stockflow.order.application;

import com.stockflow.common.exception.BadRequestException;
import com.stockflow.common.exception.ForbiddenException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.idempotency.application.IdempotencyService;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.order.api.CreateOrderRequest;
import com.stockflow.order.api.OrderDto;
import com.stockflow.order.api.OrderItemDto;
import com.stockflow.order.api.OrderItemRequest;
import com.stockflow.order.domain.Order;
import com.stockflow.order.domain.OrderItem;
import com.stockflow.order.domain.OrderStatus;
import com.stockflow.order.persistence.OrderRepository;
import com.stockflow.outbox.application.OutboxService;
import com.stockflow.product.domain.Product;
import com.stockflow.product.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService,
                        IdempotencyService idempotencyService,
                        OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
    }

    @Transactional
    public OrderDto placeOrder(String customerId, CreateOrderRequest request, String idempotencyKey) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        // 1. Check idempotency record if key provided
        String requestHash = idempotencyService.computeHash(request);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<OrderDto> existingOrder = idempotencyService.findExistingResult(
                    customerId, idempotencyKey, requestHash, OrderDto.class);
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        // 2. Consolidate quantities for duplicate product IDs in the request
        Map<UUID, Integer> consolidatedItems = new HashMap<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Each order item must specify a valid productId and positive quantity");
            }
            consolidatedItems.merge(itemReq.getProductId(), itemReq.getQuantity(), Integer::sum);
        }

        // 3. Sort product IDs deterministically to eliminate deadlocks across multi-item concurrent transactions
        List<UUID> sortedProductIds = new ArrayList<>(consolidatedItems.keySet());
        Collections.sort(sortedProductIds);

        // 4. Load active products and trusted prices from database
        Map<UUID, Product> products = new HashMap<>();
        for (UUID productId : sortedProductIds) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            if (!product.isActive()) {
                throw new BadRequestException("Cannot purchase inactive product: " + product.getSku());
            }
            products.put(productId, product);
        }

        // 5. Atomically decrement stock in deterministic order
        for (UUID productId : sortedProductIds) {
            int quantity = consolidatedItems.get(productId);
            inventoryService.decrementStock(productId, quantity, customerId, "ORDER_PLACEMENT");
        }

        // 6. Assemble Order and immutable OrderItem snapshots
        Order order = new Order(
                UUID.randomUUID(),
                customerId,
                OrderStatus.CONFIRMED,
                BigDecimal.ZERO,
                "USD"
        );

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<OrderItemDto> itemDtos = new ArrayList<>();

        for (UUID productId : sortedProductIds) {
            Product product = products.get(productId);
            int quantity = consolidatedItems.get(productId);

            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = new OrderItem(
                    UUID.randomUUID(),
                    order,
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    unitPrice,
                    quantity,
                    lineTotal
            );
            order.addItem(orderItem);

            itemDtos.add(new OrderItemDto(
                    orderItem.getId(),
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    unitPrice,
                    quantity,
                    lineTotal
            ));
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        OrderDto orderDto = new OrderDto(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDtos
        );

        // 7. Atomically record Outbox event within the same database transaction
        outboxService.recordEvent(
                "Order",
                order.getId().toString(),
                "OrderConfirmed",
                Map.of(
                        "orderId", order.getId().toString(),
                        "customerId", order.getCustomerId(),
                        "totalAmount", order.getTotalAmount().toString(),
                        "currency", order.getCurrency(),
                        "itemCount", order.getItems().size()
                )
        );

        // 8. Atomically save idempotency record if key was provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.recordSuccess(customerId, idempotencyKey, requestHash, 201, orderDto);
        }

        log.info("Successfully placed order: id={}, customerId={}, totalAmount={}",
                order.getId(), customerId, order.getTotalAmount());

        return orderDto;
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, String customerId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isAdmin && !order.getCustomerId().equals(customerId)) {
            // Documented policy: non-disclosing 404 or 403 (Handbook 04 line 65)
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getCustomerOrders(String customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    @Transactional
    public OrderDto updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(newStatus);
        order = orderRepository.save(order);
        return toDto(order);
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getId(),
                        item.getProductId(),
                        item.getSkuSnapshot(),
                        item.getNameSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDtos
        );
    }
}
