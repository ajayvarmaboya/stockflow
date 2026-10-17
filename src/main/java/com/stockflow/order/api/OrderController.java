package com.stockflow.order.api;

import com.stockflow.common.security.SecurityUtils;
import com.stockflow.order.application.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> placeOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        String customerId = SecurityUtils.getCurrentUserId().toString();
        OrderDto order = orderService.placeOrder(customerId, request, idempotencyKey);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/orders/{id}")
                .buildAndExpand(order.getId())
                .toUri();

        return ResponseEntity.created(location).body(order);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        String customerId = SecurityUtils.getCurrentUserId().toString();
        boolean isAdmin = SecurityUtils.isAdmin();
        OrderDto order = orderService.getOrder(id, customerId, isAdmin);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/me/orders")
    public ResponseEntity<Page<OrderDto>> getMyOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String customerId = SecurityUtils.getCurrentUserId().toString();
        Page<OrderDto> orders = orderService.getCustomerOrders(customerId, pageable);
        return ResponseEntity.ok(orders);
    }
}
