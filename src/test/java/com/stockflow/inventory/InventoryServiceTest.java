package com.stockflow.inventory;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.api.InventoryDto;
import com.stockflow.inventory.application.InventoryService;
import com.stockflow.inventory.domain.Inventory;
import com.stockflow.inventory.domain.InventoryAdjustment;
import com.stockflow.inventory.persistence.InventoryAdjustmentRepository;
import com.stockflow.inventory.persistence.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryAdjustmentRepository adjustmentRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        inventory = new Inventory(productId, 20);
    }

    @Test
    void adjustStock_PositiveDelta_SucceedsAndRecordsAudit() {
        when(inventoryRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryDto result = inventoryService.adjustStock(productId, 10, "RESTOCK", "admin");

        assertEquals(30, result.getQuantity());
        verify(inventoryRepository).save(inventory);
        verify(adjustmentRepository).save(any(InventoryAdjustment.class));
    }

    @Test
    void adjustStock_NegativeDeltaBelowZero_ThrowsInsufficientStockException() {
        when(inventoryRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(inventory));

        // Attempting to deduct 25 when available is 20
        assertThrows(InsufficientStockException.class, () ->
                inventoryService.adjustStock(productId, -25, "DAMAGED", "admin"));

        // Verify quantity was not altered and no save occurred
        assertEquals(20, inventory.getQuantity());
        verify(inventoryRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void decrementStock_Successful_DecrementsAndAudits() {
        when(inventoryRepository.decrementStockIfAvailable(eq(productId), eq(5), any())).thenReturn(1);

        inventoryService.decrementStock(productId, 5, "customer-1", "ORDER_PLACEMENT");

        verify(adjustmentRepository).save(any(InventoryAdjustment.class));
    }

    @Test
    void decrementStock_InsufficientStock_ThrowsInsufficientStockException() {
        when(inventoryRepository.decrementStockIfAvailable(eq(productId), eq(50), any())).thenReturn(0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () ->
                inventoryService.decrementStock(productId, 50, "customer-1", "ORDER_PLACEMENT"));

        verify(adjustmentRepository, never()).save(any());
    }
}
