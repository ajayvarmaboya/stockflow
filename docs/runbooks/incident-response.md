# StockFlow — Incident Response & Operational Runbooks

Handbook 10 compliant operational runbooks for diagnosing and mitigating production incidents.

---

## Runbook 1: Elevated Order Conflicts (409 Spike)

### Symptoms
- Increased 409 responses on `POST /api/v1/orders`.
- Client logs showing `INSUFFICIENT_STOCK` or `IDEMPOTENCY_CONFLICT`.

### Diagnostic Steps
1. Inspect metric `http_server_requests_seconds_count{status="409"}` by endpoint.
2. Check stock on affected products:
   ```sql
   SELECT p.sku, p.name, i.quantity 
   FROM products p 
   JOIN inventory i ON p.id = i.product_id 
   WHERE i.quantity <= 0;
   ```
3. Inspect `idempotency_records` for key reuse:
   ```sql
   SELECT scope, idempotency_key, count(*) 
   FROM idempotency_records 
   GROUP BY scope, idempotency_key 
   HAVING count(*) > 1;
   ```

### Mitigation
- If stock is naturally exhausted during flash sales, verify no negative stock exists (`SELECT * FROM inventory WHERE quantity < 0`). If 0 rows return, the system is behaving correctly.
- Notify customer operations / merchandising team to restock via `POST /api/v1/admin/products/{id}/stock-adjustments`.

---

## Runbook 2: Outbox Backlog / Stuck Publisher

### Symptoms
- Increasing row count in `outbox_events` with `status = 'PENDING'`.
- Oldest pending event timestamp is greater than 60 seconds old.

### Diagnostic Steps
1. Query outbox status distribution:
   ```sql
   SELECT status, count(*), min(created_at) AS oldest 
   FROM outbox_events 
   GROUP BY status;
   ```
2. Check application logs for outbox dispatch exceptions:
   ```bash
   docker logs stockflow-app | grep "Failed to publish outbox event"
   ```

### Mitigation
- Verify downstream broker connectivity and credentials.
- Inspect events marked `FAILED` (`attempts >= 5`). Address payload or schema mismatch before manually resetting status:
  ```sql
  UPDATE outbox_events SET status = 'PENDING', attempts = 0 WHERE status = 'FAILED' AND attempts >= 5;
  ```
