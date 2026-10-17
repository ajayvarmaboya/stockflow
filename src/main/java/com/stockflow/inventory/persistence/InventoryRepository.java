package com.stockflow.inventory.persistence;

import com.stockflow.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByIdForUpdate(@Param("productId") UUID productId);

    /**
     * Atomic conditional decrement (Handbook 05 line 34).
     * Returns 1 if stock was successfully decremented, 0 if insufficient stock.
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :qty, i.updatedAt = :now, i.version = i.version + 1 " +
           "WHERE i.productId = :productId AND i.quantity >= :qty")
    int decrementStockIfAvailable(@Param("productId") UUID productId, @Param("qty") int qty, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :delta, i.updatedAt = :now, i.version = i.version + 1 " +
           "WHERE i.productId = :productId AND (i.quantity + :delta) >= 0")
    int adjustStockIfValid(@Param("productId") UUID productId, @Param("delta") int delta, @Param("now") LocalDateTime now);
}
