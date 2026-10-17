package com.stockflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockflow.auth.api.AuthRequest;
import com.stockflow.auth.api.AuthResponse;
import com.stockflow.inventory.domain.Inventory;
import com.stockflow.inventory.persistence.InventoryRepository;
import com.stockflow.order.api.CreateOrderRequest;
import com.stockflow.order.api.OrderItemRequest;
import com.stockflow.product.api.CreateProductRequest;
import com.stockflow.product.domain.Product;
import com.stockflow.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndOrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private String customerToken;
    private String adminToken;
    private UUID testProductId;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate demo customer
        AuthRequest customerLogin = new AuthRequest("customer1", "Customer@123");
        MvcResult custResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerLogin)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse custAuth = objectMapper.readValue(custResult.getResponse().getContentAsString(), AuthResponse.class);
        customerToken = custAuth.getToken();

        // Authenticate admin
        AuthRequest adminLogin = new AuthRequest("admin", "Admin@123");
        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse adminAuth = objectMapper.readValue(adminResult.getResponse().getContentAsString(), AuthResponse.class);
        adminToken = adminAuth.getToken();

        // Create test product with known stock
        Product product = productRepository.save(new Product(
                UUID.randomUUID(), "SKU-API-" + UUID.randomUUID().toString().substring(0, 6),
                "API Test Widget", "For API tests", new BigDecimal("19.99"), true
        ));
        testProductId = product.getId();
        inventoryRepository.save(new Inventory(testProductId, 5));
    }

    @Test
    @DisplayName("Public catalogue browsing GET /api/v1/products returns 200 without authentication")
    void publicCatalogue_IsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("Protected order placement without token returns 401 Unauthorized")
    void unauthenticatedOrder_Returns401() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(testProductId, 1))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer calling admin product creation route returns 403 Forbidden")
    void customerCallingAdminRoute_Returns403() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "SKU-ADMIN-TEST", "Admin Widget", "Desc", new BigDecimal("100.00"), 5
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin calling admin product creation route returns 201 Created")
    void adminCallingAdminRoute_Returns201() throws Exception {
        CreateProductRequest req = new CreateProductRequest(
                "SKU-ADMIN-OK-" + UUID.randomUUID().toString().substring(0, 6),
                "Admin Approved Widget", "Desc", new BigDecimal("75.00"), 20
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", notNullValue()))
                .andExpect(jsonPath("$.price", is(75.0)));
    }

    @Test
    @DisplayName("Customer placing valid order returns 201 Created with Location header and OrderDto")
    void customerPlacingValidOrder_Returns201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(testProductId, 2))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "order-api-test-key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.totalAmount", is(39.98)))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("Ordering more than available stock returns 409 Conflict with stable error code")
    void orderExceedingStock_Returns409Conflict() throws Exception {
        // testProductId has only 5 in stock; request 100
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(testProductId, 100))
        );

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")))
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }
}
