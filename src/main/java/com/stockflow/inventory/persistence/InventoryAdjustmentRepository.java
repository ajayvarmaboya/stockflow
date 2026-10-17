package com.stockflow.inventory.persistence;

import com.stockflow.inventory.domain.InventoryAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {
    List<InventoryAdjustment> findByProductIdOrderByCreatedAtDesc(UUID productId);
    Page<InventoryAdjustment> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
    Page<InventoryAdjustment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
