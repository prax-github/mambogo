# 🚀 Mambogo E-commerce Microservices - Comprehensive Interview Guide

**Project**: Mambogo E-commerce Microservices MVP  
**Architecture**: Event-Driven Microservices with Spring Boot  
**Security**: OAuth2/OIDC + JWT + Advanced Security Headers  
**Infrastructure**: Docker + Kubernetes + Monitoring Stack  
**Date**: January 2025  
**Status**: Production-Ready MVP  

---

## 📋 Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Microservices Deep Dive](#microservices-deep-dive)
3. [Security Implementation](#security-implementation)
4. [Data Architecture & Consistency](#data-architecture--consistency)
5. [Event-Driven Architecture](#event-driven-architecture)
6. [Performance & Scalability](#performance--scalability)
7. [Monitoring & Observability](#monitoring--observability)
8. [Deployment & DevOps](#deployment--devops)
9. [Testing & Quality Assurance](#testing--quality-assurance)
10. [Production Readiness](#production-readiness)
11. [Common Interview Questions](#common-interview-questions)
12. [System Design Scenarios](#system-design-scenarios)

---

## 🏗️ System Architecture Overview

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  React SPA (Vite)                                                         │
│  • Local: localhost:5173                                                  │
│  • Demo: demo.mambogo.com                                                 │
│  • Prod: www.mambogo.com                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTPS + OAuth2/OIDC
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY LAYER                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Spring Cloud Gateway (Port 8080)                                         │
│  • OAuth2 Resource Server + JWT Validation                               │
│  • Rate Limiting (Redis-based)                                           │
│  • Input Sanitization & Threat Detection                                 │
│  • CORS + CSP Headers + Security Headers                                 │
│  • Request Routing & Load Balancing                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Service Discovery (Eureka)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MICROSERVICES LAYER                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │
│  │  Product   │ │    Cart     │ │    Order    │ │   Payment   │         │
│  │  Service   │ │   Service   │ │   Service   │ │   Service   │         │
│  │ (Port 8081)│ │ (Port 8082) │ │ (Port 8083) │ │ (Port 8084) │         │
│  │   MySQL    │ │    Redis    │ │ PostgreSQL  │ │ PostgreSQL  │         │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘         │
│                                                                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                         │
│  │ Inventory  │ │   Config    │ │   Eureka    │                         │
│  │  Service   │ │   Server    │ │   Server    │                         │
│  │ (Port 8085)│ │ (Port 8888) │ │ (Port 8761) │                         │
│  │ PostgreSQL │ │   Git       │ │  Discovery  │                         │
│  └─────────────┘ └─────────────┘ └─────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Kafka Events
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      EVENT STREAMING LAYER                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  Apache Kafka + Zookeeper                                                 │
│  • Topics: order.events, payment.events, inventory.events                │
│  • Dead Letter Queues (DLQ) for failed events                            │
│  • Event Schema Registry & Validation                                    │
│  • Outbox Pattern for Reliable Event Publishing                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Analytics Pipeline
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MONITORING LAYER                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  • Zipkin (Distributed Tracing)                                          │
│  • Prometheus + Grafana (Metrics & Dashboards)                           │
│  • ELK Stack Ready (Log Aggregation)                                     │
│  • Health Checks & Circuit Breakers                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Technology Stack Matrix

| Layer | Technology | Version | Purpose | Production Ready |
|-------|------------|---------|---------|------------------|
| **Frontend** | React + Vite | Latest | SPA with OAuth2 | ✅ |
| **Gateway** | Spring Cloud Gateway | 4.x | API Gateway + Security | ✅ |
| **Services** | Spring Boot | 3.x | Microservices Framework | ✅ |
| **Security** | Keycloak + OAuth2 | 24.0 | Identity Management | ✅ |
| **Databases** | PostgreSQL + MySQL + Redis | 15.x + 8.x + 7.x | Data Persistence | ✅ |
| **Messaging** | Apache Kafka | 7.6.0 | Event Streaming | ✅ |
| **Discovery** | Netflix Eureka | 4.x | Service Discovery | ✅ |
| **Config** | Spring Config Server | 4.x | Configuration Management | ✅ |
| **Monitoring** | Zipkin + Prometheus | Latest | Observability | ✅ |
| **Container** | Docker + Kubernetes | Latest | Deployment & Orchestration | ✅ |

---

## 🔍 Microservices Deep Dive

### 1. Product Service Architecture

#### Service Overview
- **Purpose**: Product catalog management and search
- **Database**: MySQL with full-text search capabilities
- **APIs**: Public read-only endpoints for product browsing
- **Security**: No authentication required (public service)

#### Database Schema Design

```sql
-- Core Product Table with Advanced Features
CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category_id UUID REFERENCES categories(id),
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB, -- Flexible metadata storage
    search_vector tsvector, -- Full-text search optimization
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Performance Indexes
    INDEX idx_category_active (category_id, is_active),
    INDEX idx_price_stock (price, stock_quantity),
    INDEX idx_search_vector USING GIN (search_vector),
    INDEX idx_created_at (created_at)
);

-- Full-Text Search Function
CREATE OR REPLACE FUNCTION update_product_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for automatic search vector updates
CREATE TRIGGER product_search_vector_update
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_product_search_vector();
```

#### Service Implementation

```java
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {
    
    private final ProductService productService;
    private final InputSanitizer inputSanitizer;
    
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        // Input sanitization for search terms
        String sanitizedSearch = inputSanitizer.sanitizeString(search);
        
        // Business logic validation
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Invalid pagination parameters");
        }
        
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
            .searchTerm(sanitizedSearch)
            .categoryId(categoryId)
            .pageable(PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy)))
            .build();
        
        Page<ProductResponse> products = productService.searchProducts(criteria);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable @ValidUUID String id) {
        ProductResponse product = productService.getProductById(UUID.fromString(id));
        return ResponseEntity.ok(product);
    }
}
```

### 2. Cart Service Architecture

#### Service Overview
- **Purpose**: Shopping cart management with Redis
- **Database**: Redis with TTL and atomic operations
- **APIs**: User-specific cart operations (secured)
- **Security**: JWT validation + user context

#### Redis Data Model

```redis
# Cart Key Pattern: cart:{userId}
# TTL: 30 days (automatic expiration)

# Example Cart Data Structure
cart:user-123 = {
  "userId": "user-123",
  "items": [
    {
      "productId": "prod-456",
      "quantity": 2,
      "price": 29.99,
      "addedAt": "2025-01-27T10:30:00Z",
      "productName": "Wireless Headphones"
    }
  ],
  "totalAmount": 59.98,
  "itemCount": 2,
  "updatedAt": "2025-01-27T10:30:00Z"
}

# Cart Lock for Concurrency Control
cart:lock:user-123 = "lock-token-xyz" (TTL: 30 seconds)

# Cart Metrics for Analytics
cart:metrics:user-123:2025-01-27 = {
  "addOperations": 5,
  "removeOperations": 2,
  "updateOperations": 3,
  "totalValue": 159.95
}
```

#### Service Implementation

```java
@Service
@Transactional
public class CartService {
    
    private final RedisTemplate<String, Cart> redisTemplate;
    private final ProductServiceClient productServiceClient;
    private final CartValidator cartValidator;
    
    public Cart addItemToCart(String userId, AddCartItemRequest request) {
        // Acquire distributed lock
        String lockKey = "cart:lock:" + userId;
        String lockToken = UUID.randomUUID().toString();
        
        try {
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(30));
            
            if (!lockAcquired) {
                throw new CartConcurrencyException("Cart is currently being modified");
            }
            
            // Validate product exists and has sufficient stock
            ProductResponse product = productServiceClient.getProduct(request.getProductId());
            cartValidator.validateProductAvailability(product, request.getQuantity());
            
            // Get current cart or create new one
            Cart cart = getCart(userId);
            cart.addItem(request.getProductId(), request.getQuantity(), product.getPrice());
            
            // Save cart with TTL
            String cartKey = "cart:" + userId;
            redisTemplate.opsForValue().set(cartKey, cart, Duration.ofDays(30));
            
            // Update metrics
            updateCartMetrics(userId, "add", request.getQuantity() * product.getPrice());
            
            return cart;
            
        } finally {
            // Release lock
            if (lockToken.equals(redisTemplate.opsForValue().get(lockKey))) {
                redisTemplate.delete(lockKey);
            }
        }
    }
    
    private void updateCartMetrics(String userId, String operation, BigDecimal value) {
        String metricsKey = "cart:metrics:" + userId + ":" + LocalDate.now();
        
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                
                operations.opsForHash().increment(metricsKey, operation + "Operations", 1);
                operations.opsForHash().increment(metricsKey, "totalValue", value.doubleValue());
                operations.expire(metricsKey, Duration.ofDays(90));
                
                return operations.exec();
            }
        });
    }
}
```

### 3. Order Service Architecture

#### Service Overview
- **Purpose**: Order processing with saga pattern
- **Database**: PostgreSQL with outbox pattern
- **APIs**: Order creation, management, and status updates
- **Security**: JWT validation + business rule enforcement

#### Database Schema with Outbox Pattern

```sql
-- Orders Table with Business Rules
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'SHIPPED', 'DELIVERED')),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount > 0),
    shipping_address JSONB NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(64) UNIQUE,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 minutes'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Business Rule Constraints
    CONSTRAINT min_order_amount CHECK (total_amount >= 10.00),
    CONSTRAINT max_order_amount CHECK (total_amount <= 10000.00),
    CONSTRAINT order_timeout CHECK (expires_at > created_at),
    
    -- Performance Indexes
    INDEX idx_user_status (user_id, status),
    INDEX idx_status_expires (status, expires_at),
    INDEX idx_idempotency (idempotency_key),
    INDEX idx_created_at (created_at)
);

-- Outbox Table for Reliable Event Publishing
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    headers JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY')),
    
    -- Performance & Monitoring Indexes
    INDEX idx_status_created (status, created_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_retry (next_retry_at) WHERE status = 'RETRY'
);

-- Order Items with Pricing Details
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0 AND quantity <= 50),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price > 0),
    product_name VARCHAR(255) NOT NULL,
    product_image_url VARCHAR(500),
    
    -- Business Rule Constraints
    CONSTRAINT max_items_per_order CHECK (quantity <= 50),
    CONSTRAINT price_consistency CHECK (total_price = unit_price * quantity),
    
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);
```

#### Saga Pattern Implementation

```java
@Service
@Transactional
public class OrderSagaService {
    
    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryService;
    private final PaymentServiceClient paymentService;
    private final OutboxEventService outboxService;
    private final IdempotencyService idempotencyService;
    
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // Idempotency check
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getCachedResponse(idempotencyKey);
        }
        
        // Validate business rules
        validateOrderBusinessRules(request);
        
        // Create order in PENDING state
        Order order = createPendingOrder(userId, request);
        
        try {
            // Step 1: Reserve inventory
            InventoryReservationResponse inventoryResponse = 
                inventoryService.reserveInventory(order.getId(), request.getItems());
            
            if (!inventoryResponse.isSuccess()) {
                throw new InventoryReservationException("Insufficient inventory");
            }
            
            // Step 2: Process payment
            PaymentResponse paymentResponse = 
                paymentService.processPayment(order.getId(), order.getTotalAmount());
            
            if (paymentResponse.isSuccess()) {
                // Step 3: Confirm order
                order.confirm();
                orderRepository.save(order);
                
                // Publish order confirmed event
                publishOrderConfirmedEvent(order);
                
                // Cache successful response
                OrderResponse response = mapToOrderResponse(order);
                idempotencyService.cacheResponse(idempotencyKey, response);
                
                return response;
                
            } else {
                // Payment failed - compensate inventory
                inventoryService.releaseInventory(order.getId());
                throw new PaymentProcessingException("Payment processing failed");
            }
            
        } catch (Exception e) {
            // Saga failure - cancel order
            order.cancel(e.getMessage());
            orderRepository.save(order);
            
            // Publish order cancelled event
            publishOrderCancelledEvent(order, e.getMessage());
            
            throw e;
        }
    }
    
    private void publishOrderConfirmedEvent(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .totalAmount(order.getTotalAmount())
            .confirmedAt(Instant.now())
            .build();
        
        outboxService.saveEvent("Order", order.getId().toString(), 
            "OrderConfirmed", event, Map.of("version", "1.0"));
    }
}
```

---

## 🔒 Security Implementation

### 1. OAuth2/OIDC Architecture

#### Authentication Flow Diagram

```
┌─────────────┐   1. User Login    ┌─────────────┐
│   Client    │ ──────────────────► │  Keycloak   │
│  (Browser)  │                    │   Realm     │
└─────────────┘                    └─────────────┘
       │                                   │
       │ 2. Authorization Code             │
       │    + PKCE Challenge              │
       │ ◄─────────────────────────────────│
       │                                   │
       │ 3. Exchange Code for Tokens      │
