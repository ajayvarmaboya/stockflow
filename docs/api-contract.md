# StockFlow — REST API Contract

Base URL: `/api/v1`

## Authentication

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Public | Register new customer or admin user |
| `POST` | `/auth/login` | Public | Authenticate with username and password, returns Bearer token |

### Login Example
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "customer1",
  "password": "Customer@123"
}
```
Response: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "d748f342-9f33-4f18-bb65-0a37e1a38210",
  "username": "customer1",
  "role": "CUSTOMER"
}
```

---

## Products & Catalogue

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/products` | Public | Paginated list of active products |
| `GET` | `/products/{id}` | Public | Active product details with available stock |
| `POST` | `/admin/products` | `ADMIN` | Create product and initialize stock |
| `PATCH` | `/admin/products/{id}` | `ADMIN` | Update product details or active state |
| `GET` | `/admin/products` | `ADMIN` | Paginated list of all products (including inactive) |
| `POST` | `/admin/products/{id}/stock-adjustments` | `ADMIN` | Adjust stock with delta and audit reason |
| `GET` | `/admin/products/{id}/stock-adjustments` | `ADMIN` | View audit trail of stock adjustments |

---

## Orders

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/orders` | Authenticated | Place order (supports `Idempotency-Key` header) |
| `GET` | `/orders/{id}` | Owner / Admin | Retrieve order details |
| `GET` | `/me/orders` | Authenticated | View authenticated customer's order history |
| `GET` | `/admin/orders` | `ADMIN` | View all customer orders across the platform |
| `PATCH` | `/admin/orders/{id}/status` | `ADMIN` | Update order lifecycle status |

### Order Placement Example
```http
POST /api/v1/orders
Authorization: Bearer <token>
Idempotency-Key: c9d8e7a6-b5c4-3d2e-1f0a-9876543210ab
Content-Type: application/json

{
  "items": [
    { "productId": "550e8400-e29b-41d4-a716-446655440000", "quantity": 2 }
  ]
}
```
Response: `201 Created`
```json
{
  "id": "e2a1b0c9-8d7e-6f5a-4b3c-2d1e0f9a8b7c",
  "customerId": "d748f342-9f33-4f18-bb65-0a37e1a38210",
  "status": "CONFIRMED",
  "totalAmount": 259.98,
  "currency": "USD",
  "createdAt": "2026-10-17T14:18:23.348",
  "updatedAt": "2026-10-17T14:18:23.348",
  "items": [
    {
      "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "sku": "SKU-WIDGET-01",
      "name": "Premium Mechanical Keyboard",
      "unitPrice": 129.99,
      "quantity": 2,
      "lineTotal": 259.98
    }
  ]
}
```

---

## Standard Error Response Format

All error responses strictly adhere to the following schema:
```json
{
  "timestamp": "2026-10-17T14:18:24.000Z",
  "status": 409,
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for product 550e8400-e29b-41d4-a716-446655440000. Available: 1, Requested: 2",
  "path": "/api/v1/orders",
  "correlationId": "5d31a1e5-136b-46ee-9722-04b522c02be2"
}
```
