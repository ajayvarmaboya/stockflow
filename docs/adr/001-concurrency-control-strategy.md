# ADR 001: Concurrency Control Strategy for Inventory Management

## Status
Accepted

## Context
When multiple concurrent customers attempt to purchase the remaining inventory of a popular product, race conditions can cause **overselling** (inventory dropping below zero). We needed a concurrency strategy that balances data integrity with high throughput and low latency.

## Decision
We implemented a **two-tier defense**:
1. **Atomic Conditional Updates**:
   `UPDATE inventory SET quantity = quantity - :qty WHERE product_id = :id AND quantity >= :qty`
   - Executes directly within PostgreSQL's row locking mechanism.
   - If the update returns 0 affected rows, an `InsufficientStockException` is thrown and the entire order transaction is rolled back.
2. **Deterministic Locking Order**:
   - For multi-item orders, product IDs are sorted alphabetically by UUID before row updates occur.
   - This eliminates circular wait conditions and prevents database deadlocks under high concurrent load.

## Consequences
- **Positive**: Zero possibility of overselling; inventory strictly obeys `CHECK (quantity >= 0)`.
- **Positive**: Minimal lock hold times compared to coarse table locks or distributed locks.
- **Trade-off**: High contention on a single SKU under flash-sale spikes may result in higher conflict rates (409 Conflict) for losing requests, which is correct business behavior.
