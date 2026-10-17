package com.stockflow.inventory.application;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.api.InventoryDto;
import com.stockflow.inventory.api.StockAdjustmentDto;
import com.stockflow.inventory.domain.Inventory;
import com.stockflow.inventory.domain.InventoryAdjustment;
import com.stockflow.inventory.persistence.InventoryAdjustmentRepository;
import com.stockflow.inventory.persistence.InventoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;

    public InventoryService(InventoryRepository inventoryRepository, InventoryAdjustmentRepository adjustmentRepository) {
        this.inventoryRepository = inventoryRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Transactional(readOnly = true)
    public InventoryDto getInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return toDto(inventory);
    }

    @Transactional
    public void initializeStock(UUID productId, int initialQuantity) {
        Inventory inventory = new Inventory(productId, initialQuantity);
        inventoryRepository.save(inventory);

        if (initialQuantity > 0) {
            InventoryAdjustment adjustment = new InventoryAdjustment(
                    UUID.randomUUID(),
                    productId,
                    initialQuantity,
                    "INITIAL_STOCK",
                    "SYSTEM"
            );
            adjustmentRepository.save(adjustment);
        }
    }

    @Transactional
    public InventoryDto adjustStock(UUID productId, int delta, String reason, String actorId) {
        Inventory inventory = inventoryRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product: " + productId));

        int newQuantity = inventory.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new InsufficientStockException(
                    "Cannot adjust stock. Resulting quantity would be negative (" + newQuantity + ") for product: " + productId
            );
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        InventoryAdjustment adjustment = new InventoryAdjustment(
                UUID.randomUUID(),
                productId,
                delta,
                reason,
                actorId != null ? actorId : "SYSTEM"
        );
        adjustmentRepository.save(adjustment);

        return toDto(inventory);
    }

    /**
     * Decrements stock within an existing order transaction.
     * Uses atomic conditional decrement or row-level lock.
     */
    @Transactional
    public void decrementStock(UUID productId, int quantity, String actorId, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to decrement must be positive");
        }

        int updatedRows = inventoryRepository.decrementStockIfAvailable(productId, quantity, LocalDateTime.now());
        if (updatedRows == 0) {
            // Check if inventory exists at all
            Inventory inventory = inventoryRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product inventory not found for: " + productId));
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + ". Available: " + inventory.getQuantity() + ", Requested: " + quantity
            );
        }

        InventoryAdjustment adjustment = new InventoryAdjustment(
                UUID.randomUUID(),
                productId,
                -quantity,
                reason,
                actorId
        );
        adjustmentRepository.save(adjustment);
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentDto> getAdjustmentHistory(UUID productId) {
        return adjustmentRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toAdjustmentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StockAdjustmentDto> getAdjustmentHistory(UUID productId, Pageable pageable) {
        return adjustmentRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toAdjustmentDto);
    }

    private InventoryDto toDto(Inventory entity) {
        return new InventoryDto(entity.getProductId(), entity.getQuantity(), entity.getVersion(), entity.getUpdatedAt());
    }

    private StockAdjustmentDto toAdjustmentDto(InventoryAdjustment entity) {
        return new StockAdjustmentDto(
                entity.getId(),
                entity.getProductId(),
                entity.getDelta(),
                entity.getReason(),
                entity.getActorId(),
                entity.getCreatedAt()
        );
    }
}
