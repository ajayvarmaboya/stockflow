# StockFlow — System Architecture & Design

StockFlow is architected as a **modular monolith** with clear domain boundaries, strong transactional consistency, and idempotent interfaces.

```
Client (Web / Mobile / CLI)
           │
           ▼
   Spring Security (JWT Bearer Auth)
           │
           ▼
   REST Controllers (/api/v1/...)
           │
           ▼
    Application Services (Product, Inventory, Order, Idempotency, Outbox)
           │
           ▼
    Domain Rules & Invariant Enforcement
           │
           ▼
    Repositories (Spring Data JPA)
           │
           ▼
    Durable Storage: PostgreSQL (Transactional Source of Truth)
```

## Module Boundaries

1. **`product`**: Owns product SKU normalization, name, description, price (using `BigDecimal`), and active status.
2. **`inventory`**: Owns on-hand quantity, atomic conditional decrements, and immutable audit logs (`inventory_adjustments`).
3. **`order`**: Owns order lifecycle (`CONFIRMED`, `FULFILLED`, `CANCELLED`), server-side price calculation, and immutable snapshots of product name, SKU, and unit price.
4. **`idempotency`**: Owns canonical request hashing (SHA-256), replay caching, and duplicate conflict detection.
5. **`outbox`**: Owns asynchronous event publishing intent (`outbox_events`), ensuring zero dual-write divergence between database commits and broker dispatch.
6. **`auth` & `security`**: Owns BCrypt password hashing, JWT creation/validation, and role-based access control (`CUSTOMER`, `ADMIN`).

## Order Placement Sequence

```
Client                OrderService             InventoryService        Database
  │                         │                         │                    │
  │─── POST /api/v1/orders ─▶                         │                    │
  │    (Idempotency-Key)    │                         │                    │
  │                         │── Check Idempotency ────┼───────────────────▶│
  │                         │                         │                    │
  │                         │── Sort Product IDs ─────┼────────────────────│ (Prevents deadlocks)
  │                         │                         │                    │
  │                         │── Fetch Trusted Prices ─┼───────────────────▶│
  │                         │                         │                    │
  │                         │── Decrement Stock ─────▶│                    │
  │                         │   (Conditional Update)  │── Atomic Update ──▶│ (quantity >= qty)
  │                         │                         │                    │
  │                         │── Persist Order & Items ┼───────────────────▶│
  │                         │── Write Outbox Event ───┼───────────────────▶│ (Same DB Tx)
  │                         │── Save Idempotency Record ──────────────────▶│
  │                         │                         │                    │
  │◀── 201 Created (Order) ─│                         │                    │
```

## Invariants Guaranteed by Architecture

1. **No Overselling**: Row-level locking and atomic conditional updates (`UPDATE ... WHERE quantity >= :qty`) ensure that even under extreme concurrency, available stock never becomes negative.
2. **Immutable History**: Orders store frozen snapshots of product name, SKU, and unit price at the time of purchase. Future catalogue edits or price changes do not distort historical orders.
3. **Zero Dual-Write Inconsistency**: Business updates (order + inventory) and outbox events commit together in a single PostgreSQL ACID transaction.
