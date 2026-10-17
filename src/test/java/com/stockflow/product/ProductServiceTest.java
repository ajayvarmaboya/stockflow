package com.stockflow.product;

import com.stockflow.common.exception.ConflictException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.inventory.domain.Inventory;
import com.stockflow.inventory.persistence.InventoryRepository;
import com.stockflow.product.api.CreateProductRequest;
import com.stockflow.product.api.ProductDto;
import com.stockflow.product.api.UpdateProductRequest;
import com.stockflow.product.application.ProductService;
import com.stockflow.product.domain.Product;
import com.stockflow.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ProductService productService;

    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateProductRequest("sku-100", "Gaming Mouse", "Ergonomic mouse", new BigDecimal("49.99"), 10);
    }

    @Test
    void createProduct_NormalizesSkuAndInitializesStock() {
        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto result = productService.createProduct(createRequest);

        assertNotNull(result);
        assertEquals("SKU-100", result.getSku());
        assertEquals("Gaming Mouse", result.getName());
        assertEquals(new BigDecimal("49.99"), result.getPrice());
        assertEquals(10, result.getAvailableStock());

        verify(inventoryService).initializeStock(result.getId(), 10);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_ThrowsConflictOnDuplicateSku() {
        when(productRepository.existsBySku("SKU-100")).thenReturn(true);

        assertThrows(ConflictException.class, () -> productService.createProduct(createRequest));
        verify(productRepository, never()).save(any());
        verify(inventoryService, never()).initializeStock(any(), anyInt());
    }

    @Test
    void updateProduct_UpdatesFieldsCorrectly() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, "SKU-100", "Old Name", "Old Desc", new BigDecimal("40.00"), true);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(new Inventory(productId, 15)));

        UpdateProductRequest updateRequest = new UpdateProductRequest("New Name", "New Desc", new BigDecimal("55.00"), false);
        ProductDto updated = productService.updateProduct(productId, updateRequest);

        assertEquals("New Name", updated.getName());
        assertEquals("New Desc", updated.getDescription());
        assertEquals(new BigDecimal("55.00"), updated.getPrice());
        assertFalse(updated.isActive());
        assertEquals(15, updated.getAvailableStock());
    }

    @Test
    void getProduct_ThrowsNotFoundForMissingProduct() {
        UUID randomId = UUID.randomUUID();
        when(productRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(randomId));
    }
}
