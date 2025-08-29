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

## 7\. 🔌 API Contracts

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

## 8\. 📡 Event Contracts

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

## 9\. 🧪 Testing & Validation

* **Postman collection** with flows: login, browse products, add to cart, place order, observe Kafka events.  
* **Contract testing** (consumer-driven).  
* **Chaos drills**: stop Payment DB → ensure DLQ usage.  
* **Idempotency tests**: send same `X-Idempotency-Key` twice → second call returns cached response.

---

## 10\. 🚀 Delivery & Ops

* **CI/CD**: Minimal pipeline (lint → build → test → compose up).  
* **Rollback strategy**: revert to previous Docker image tag.  
* **Database migrations**: Flyway/Liquibase placeholders.  
* **Monitoring**: Zipkin UI \+ optional Prometheus/Grafana dashboards.

---

## 11\. ⏱️ Timeline (2 Days)

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

## 12\. 🎯 Success Criteria

* User can log in via Keycloak and is redirected back.  
* User can browse products and manage cart.  
* User can place order → inventory reserved → payment charged → order confirmed.  
* Failure case: insufficient stock or payment failure → order cancelled, inventory released.  
* Kafka events logged in Analytics consumer.  
* Zipkin shows trace across services.

---

## 13\. 📦 Deliverables

* Repo: `ecommerce/` with gateway, services, frontend, docker-compose.  
* Docs: README with setup instructions.  
* Diagrams: Eraser architecture (C4: Context, Container, Component).  
* Postman collection for API tests.  
* OpenAPI contracts \+ Event Schemas.  
* ADRs (e.g., Outbox vs direct publish, JWT validation strategy).