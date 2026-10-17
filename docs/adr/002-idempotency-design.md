# ADR 002: Idempotent Order Placement Design

## Status
Accepted

## Context
In distributed environments, network timeouts can leave clients uncertain whether an order placement request succeeded. Retrying without idempotency can create duplicate orders, charge customers twice, and deduct inventory multiple times.

## Decision
We implemented **Scoped Request Hashing & Result Caching**:
1. Clients provide an `Idempotency-Key` header with each `POST /api/v1/orders` request.
2. The key is scoped by the authenticated user's ID: `(scope, idempotency_key)`.
3. The canonical JSON request payload is hashed using **SHA-256**.
4. A unique database constraint on `(scope, idempotency_key)` protects against duplicate entries.
5. If a matching record exists:
   - If the request hash matches: return the cached response payload immediately without re-executing order or stock logic.
   - If the request hash differs: reject with `409 Conflict` (`IDEMPOTENCY_CONFLICT`).

## Consequences
- **Positive**: Seamless client retries with zero duplicate orders and zero double-deductions.
- **Positive**: Detects client-side bugs where the same key is accidentally used for different shopping carts.
- **Trade-off**: Requires database storage for idempotency records (configured with a 24-hour expiration window).
