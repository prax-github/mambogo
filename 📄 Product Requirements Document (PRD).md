# 📄 Product Requirements Document (PRD)

**Project**: E-commerce Microservices MVP  
**Owner**: Prashant Sinha  
**Date**: Aug 2025  
**Timeline**: 2 days

---

## 1\. 📌 Overview

This project is a hands-on microservices-based e-commerce MVP built using **Spring Boot, Spring Cloud Gateway, Kafka, PostgreSQL, Redis, Docker Compose, and React (Vite)**.

The MVP demonstrates a **secure, event-driven architecture** that includes OAuth2/OIDC-based login (Keycloak), redirect-after-login for SPA, synchronous APIs for cart & checkout, and asynchronous Kafka events for order & payment flows.

The PRD outlines **functional requirements, non-functional requirements, architecture, data models, APIs, milestones, and success criteria.**

---

## 2\. 🎯 Goals

- Deliver a working demo in 2 days (MVP scope).  
- Cover core **interview topics**: OAuth2/OIDC, API Gateway, microservices, data consistency, Kafka events, observability.  
- Provide vertical slice:  
  - Browse products (public)  
  - Add to cart (secured)  
  - Place order (secured)  
  - Orchestrate order \+ payment \+ inventory  
  - Publish/consume events via Kafka  
  - Persist analytics

---

## 3\. ✅ Functional Requirements

### 3.1 Authentication & Security

- OAuth2 \+ OpenID Connect (via Keycloak).  
- Support login \+ redirect back to requested page in SPA.  
- JWT validation at API Gateway and microservices.  
- Role-based access (ROLE\_USER, ROLE\_ADMIN).  
- Token lifetime: 15 mins, refresh via Keycloak refresh tokens.  
- Service scopes: `product:read`, `order:write`, `cart:manage`.

### 3.2 Product Browsing

Public catalog browsing.

Endpoints:

- `GET /api/catalog/products`  
- `GET /api/catalog/products/{id}`

### 3.3 Cart

Each logged-in user has a persistent cart.

Endpoints:

- `GET /api/cart`  
- `PUT /api/cart`

### 3.4 Order & Checkout

Place an order (secured).

Flow:

1. Create order (PENDING).  
2. Reserve inventory.  
3. Charge payment.  
4. Confirm order → publish `OrderPlacedEvent`.  
5. On failure: cancel order, release inventory, refund payment.

Endpoints:

- `POST /api/orders (with X-Idempotency-Key)`  
- `GET /api/orders/me`

### 3.5 Payment

Mock payment service. Returns success/failure (90% success probability).

Endpoint:

- `POST /api/payments/charge`

### 3.6 Inventory

Reserve/release stock.

Endpoints:

- `POST /api/inventory/reserve`  
- `POST /api/inventory/release`

### 3.7 Analytics

Kafka topics feed analytics DB.

Events tracked: `order.events`, `payment.events`.  
Minimal consumer writes to Analytics table or console log.

---

## 4\. ⚙️ Non-Functional Requirements

- **Scalability**: Stateless services; horizontal scaling via Docker.  
- **Resilience**:  
  - Retry (1x), timeout (2s), idempotency keys.  
  - Circuit breakers (Resilience4j).  
  - Dead-letter queues (Kafka DLQ) for poison messages.  
  - Outbox pattern in Order service.  
- **Consistency**: Eventual consistency with compensation (release inventory if payment fails).  
- **Observability**:  
  - Spring Sleuth \+ Zipkin for tracing.  
  - Correlation IDs propagated across services.  
  - Structured JSON logs.  
  - Prometheus \+ Grafana (optional).  
- **Deployment**: Docker Compose for local stack.  
- **Performance**: Response \<500ms for sync APIs (under light load).  
- **Security**: Role-based scopes; secrets managed via Spring Config Server.

---

## 5\. 🏗️ Architecture

### Components

- **Gateway**: Spring Cloud Gateway \+ OAuth2 Resource Server.  
- **Auth**: Keycloak (OIDC \+ PKCE).  
- **Microservices**:  
  - product-service (MySQL)  
  - cart-service (Redis)  
  - order-service (Postgres)  
  - payment-service (Postgres/mock)  
  - inventory-service (Postgres)  
- **Frontend**: React (Vite) SPA.  
- **Messaging**: Kafka for async events.  
- **Analytics**: Kafka Streams → Analytics DB.

### Flows

- **Sync orchestration**: Order → Inventory → Payment (REST).  
- **Async events**: OrderPlaced → PaymentCompleted → OrderConfirmed (Kafka).  
- **Redirect-after-login**: SPA stores returnUrl in OIDC state.

---

## 6\. 🗄️ Data Models (Simplified)

**Product**  
`(id, name, description, price, stock, updated_at)`

**Cart**

{

  "userId": "u123",

  "items": \[{ "productId": "p1", "qty": 2 }\]

}

**Order** `(id, user_id, status, total, idempotency_key, created_at)`

**Payment** `(id, order_id, amount, status, payment_ref, created_at)`

**Inventory** `(product_id, available, reserved)`

**Outbox (for order-service)** `(id, aggregate_type, aggregate_id, event_type, payload, headers, created_at, sent_at)`

---

## 7\. 🗄️ Detailed Database Schema

### 7.1 Product Service (MySQL)

```sql
CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category VARCHAR(100),
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_active (is_active),
    INDEX idx_price (price)
);
```

### 7.2 Cart Service (Redis)

```redis
# Key pattern: cart:{userId}
# Value: JSON string
{
  "userId": "uuid",
  "items": [
    {
      "productId": "uuid",
      "quantity": 2,
      "price": 29.99,
      "addedAt": "2025-08-28T12:00:00Z"
    }
  ],
  "updatedAt": "2025-08-28T12:00:00Z"
}

# TTL: 30 days
```

### 7.3 Order Service (PostgreSQL)

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'SHIPPED', 'DELIVERED')),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount > 0),
    shipping_address TEXT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(64) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_idempotency_key (idempotency_key)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price > 0),
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY')),
    INDEX idx_status_created (status, created_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id)
);
```

### 7.4 Payment Service (PostgreSQL)

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'AUTHORIZED', 'FAILED', 'REFUNDED')),
    payment_method VARCHAR(50) NOT NULL,
    payment_reference VARCHAR(100),
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### 7.5 Inventory Service (PostgreSQL)

```sql
CREATE TABLE inventory (
    product_id UUID PRIMARY KEY,
    available_quantity INT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    reorder_point INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (available_quantity >= reserved_quantity)
);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES inventory(product_id),
    order_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
        CHECK (status IN ('RESERVED', 'RELEASED', 'CONFIRMED')),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_order_id (order_id),
    INDEX idx_expires_at (expires_at)
);
```

---

## 8\. 🔌 API Contracts

### Gateway

* Routes `/api/**` to backend services.  
* JWT validation.

### Product Service

* `GET /api/catalog/products`  
* `GET /api/catalog/products/{id}`

### Cart Service

* `GET /api/cart`  
* `PUT /api/cart`

### Order Service

* `POST /api/orders`  
    
  * Headers: `X-Idempotency-Key` (required)


* `GET /api/orders/me`

### Payment Service

* `POST /api/payments/charge`

### Inventory Service

* `POST /api/inventory/reserve`  
* `POST /api/inventory/release`

### Common Error Model

{

  "code": "ERR\_ORDER\_001",

  "message": "Payment failed",

  "traceId": "abc123"

}

---

## 9\. 📡 Event Contracts

### Topics

* `order.events`  
* `payment.events`  
* `payment.events.DLQ`

### Event Schemas

**OrderPlacedEvent**

{

  "eventType": "OrderPlaced",

  "orderId": "o123",

  "userId": "u123",

  "total": 199.99,

  "timestamp": "2025-08-28T12:00:00Z"

}

**PaymentAuthorizedEvent**

{

  "eventType": "PaymentAuthorized",

  "paymentId": "p123",

  "orderId": "o123",

  "status": "AUTHORIZED",

  "timestamp": "2025-08-28T12:01:00Z"

}

**PaymentFailedEvent**

{

  "eventType": "PaymentFailed",

  "paymentId": "p124",

  "orderId": "o123",

  "status": "FAILED",

  "reason": "Insufficient funds",

  "timestamp": "2025-08-28T12:02:00Z"

}

---

## 10\. 🚨 Error Handling & Business Rules

### 10.1 Error Response Format
All error responses follow this standard format:
```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "traceId": "correlation-id",
  "timestamp": "2025-08-28T12:00:00Z",
  "details": {
    "field": "additional error details"
  }
}
```

### 10.2 Common Error Codes
- `AUTHENTICATION_FAILED`: Invalid or expired JWT token
- `AUTHORIZATION_FAILED`: Insufficient permissions
- `VALIDATION_ERROR`: Request validation failed
- `RESOURCE_NOT_FOUND`: Requested resource not found
- `DUPLICATE_RESOURCE`: Resource already exists
- `BUSINESS_RULE_VIOLATION`: Business rule violation
- `SERVICE_UNAVAILABLE`: Downstream service unavailable
- `INTERNAL_ERROR`: Unexpected server error

### 10.3 Business Rules

#### Order Rules
- Minimum order amount: $10.00
- Maximum order amount: $10,000.00
- Maximum items per order: 50
- Order timeout: 30 minutes for payment completion
- Idempotency key required for order creation
- Idempotency key validity: 24 hours

#### Inventory Rules
- Cannot reserve more than available stock
- Reservation timeout: 5 minutes
- Auto-release expired reservations
- Reorder point: 10 units
- Maximum reservation per user: 5 active reservations

#### Payment Rules
- Payment must be completed within 30 minutes of order creation
- 90% success rate for mock payments
- Failed payments trigger order cancellation
- Refund processing: immediate for failed orders

#### Cart Rules
- Cart expiration: 30 days
- Maximum items per cart: 100
- Maximum quantity per item: 10
- Auto-cleanup expired carts

---

## 11\. 🧪 Testing & Validation

* **Postman collection** with flows: login, browse products, add to cart, place order, observe Kafka events.  
* **Contract testing** (consumer-driven).  
* **Chaos drills**: stop Payment DB → ensure DLQ usage.  
* **Idempotency tests**: send same `X-Idempotency-Key` twice → second call returns cached response.

---

## 12\. 🔒 Security & Compliance

### 12.1 Authentication & Authorization
- **OAuth2/OIDC**: Keycloak integration with PKCE flow
- **JWT Validation**: At API Gateway and microservices
- **Token Lifetime**: 15 minutes access, 7 days refresh
- **Scopes**: `product:read`, `order:write`, `cart:manage`, `admin:all`
- **Roles**: `ROLE_USER`, `ROLE_ADMIN`

### 12.2 API Security
- **Rate Limiting**: 100 requests/minute per user, 1000 requests/minute per IP
- **CORS**: Configured for SPA domain only
- **Input Validation**: All inputs validated and sanitized
- **SQL Injection Protection**: Parameterized queries only
- **XSS Protection**: Content Security Policy headers

### 12.3 Data Protection
- **Encryption at Rest**: Database encryption enabled
- **Encryption in Transit**: TLS 1.3 for all communications
- **PII Handling**: User data encrypted, minimal collection
- **Audit Logging**: All sensitive operations logged
- **Data Retention**: 7 years for orders, 30 days for carts

### 12.4 Compliance
- **GDPR**: Right to be forgotten, data portability
- **PCI DSS**: Payment data not stored (mock service)
- **Logging**: No sensitive data in logs
- **Monitoring**: Security events monitored and alerted

---

## 13\. 🚀 Delivery & Ops

* **CI/CD**: Minimal pipeline (lint → build → test → compose up).  
* **Rollback strategy**: revert to previous Docker image tag.  
* **Database migrations**: Flyway/Liquibase placeholders.  
* **Monitoring**: Zipkin UI \+ optional Prometheus/Grafana dashboards.

---

## 14\. ⏱️ Timeline (2 Days)

**Day 1**

* Set up repo \+ multi-module structure.  
* Docker Compose: Keycloak, Postgres, Redis, Kafka, Zipkin.  
* Implement Product \+ Cart services.  
* Configure Gateway \+ Auth.  
* React SPA: Login \+ Product listing \+ Cart page.

**Day 2**

* Implement Order \+ Inventory \+ Payment services.  
* Integrate sync orchestration for checkout.  
* Add Kafka publishers/consumers.  
* Add Analytics consumer.  
* Add observability (Zipkin).  
* Final testing \+ Postman collection.

---

## 15\. 🎯 Success Criteria

* User can log in via Keycloak and is redirected back.  
* User can browse products and manage cart.  
* User can place order → inventory reserved → payment charged → order confirmed.  
* Failure case: insufficient stock or payment failure → order cancelled, inventory released.  
* Kafka events logged in Analytics consumer.  
* Zipkin shows trace across services.

---

## 16\. 📦 Deliverables

* Repo: `ecommerce/` with gateway, services, frontend, docker-compose.  
* Docs: README with setup instructions.  
* Diagrams: Eraser architecture (C4: Context, Container, Component).  
* Postman collection for API tests.  
* OpenAPI contracts \+ Event Schemas.  
* ADRs (e.g., Outbox vs direct publish, JWT validation strategy).