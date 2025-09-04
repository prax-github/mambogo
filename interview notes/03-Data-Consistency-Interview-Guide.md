# 📊 Data Consistency & Event-Driven Architecture - Interview Guide

**Project**: Mambogo E-commerce Microservices MVP  
**Focus**: Data Consistency, Event Sourcing, Saga Pattern, Outbox Pattern  
**Level**: Senior Data Engineer / Distributed Systems Engineer  
**Date**: January 2025  

---

## 🏗️ Data Architecture Overview

### Multi-Database Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MICROSERVICES DATA LAYER                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │
│  │  Product   │ │    Cart     │ │    Order    │ │   Payment   │         │
│  │  Service   │ │   Service   │ │   Service   │ │   Service   │         │
│  │   MySQL    │ │    Redis    │ │ PostgreSQL  │ │ PostgreSQL  │         │
│  │  (Port     │ │  (Port      │ │  (Port      │ │  (Port      │         │
│  │   5433)    │ │   6379)     │ │   5434)     │ │   5435)     │         │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘         │
│                                                                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                         │
│  │ Inventory  │ │   Config    │ │   Eureka    │                         │
│  │  Service   │ │   Server    │ │   Server    │                         │
│  │ PostgreSQL │ │   Git       │ │  Discovery  │                         │
│  │ (Port      │ │             │ │  (Port      │                         │
│  │   5436)    │ │             │ │   8761)     │                         │
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
```

---

## 🔄 Event-Driven Architecture Patterns

### **1. Saga Pattern for Distributed Transactions**

#### **Order Creation Saga Flow**

```
┌─────────────┐   1. Create Order    ┌─────────────┐
│   Client    │ ──────────────────► │   Order     │
│             │                     │  Service    │
└─────────────┘                     └─────────────┘
                                             │
                                             │ 2. Order Created Event
                                             ▼
┌─────────────┐   3. Reserve Inventory    ┌─────────────┐
│ Inventory   │ ◄───────────────────────── │   Kafka    │
│  Service    │                            │  Event     │
└─────────────┘                            │   Bus      │
       │                                   └─────────────┘
       │ 4. Inventory Reserved Event
       ▼
┌─────────────┐   5. Process Payment    ┌─────────────┐
│  Payment    │ ◄─────────────────────── │   Kafka    │
│  Service    │                          │  Event     │
└─────────────┘                          │   Bus      │
       │                                 └─────────────┘
       │ 6. Payment Processed Event
       ▼
┌─────────────┐   7. Confirm Order    ┌─────────────┐
│   Order     │ ◄───────────────────── │   Kafka    │
│  Service    │                        │  Event     │
└─────────────┘                        │   Bus      │
                                       └─────────────┘
```

#### **Saga Implementation with Compensation**

```java
@Service
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

### **2. Outbox Pattern for Reliable Event Publishing**

#### **Outbox Table Schema**

```sql
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

#### **Outbox Service Implementation**

```java
@Service
public class OutboxEventService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void saveEvent(String aggregateType, String aggregateId, 
                         String eventType, Object payload, Map<String, String> headers) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(payload))
                .headers(objectMapper.writeValueAsString(headers))
                .createdAt(LocalDateTime.now())
                .status(OutboxEventStatus.PENDING)
                .build();
            
            outboxEventRepository.save(event);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
    
    @Scheduled(fixedRate = 5000)
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
            .findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
        
        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish to Kafka
                kafkaTemplate.send(event.getEventType(), event.getPayload());
                
                // Mark as sent
                event.markAsSent();
                outboxEventRepository.save(event);
                
                log.info("Event published successfully: {} - {}", event.getEventType(), event.getAggregateId());
                
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                
                // Increment retry count
                event.incrementRetryCount();
                
                if (event.getRetryCount() >= event.getMaxRetries()) {
                    event.setStatus(OutboxEventStatus.FAILED);
                    log.error("Event failed permanently after {} retries: {}", 
                        event.getMaxRetries(), event.getId());
                } else {
                    event.setStatus(OutboxEventStatus.RETRY);
                    event.setNextRetryAt(calculateNextRetryTime(event.getRetryCount()));
                }
                
                outboxEventRepository.save(event);
            }
        }
    }
    
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // Exponential backoff: 5s, 10s, 20s, 40s
        long delaySeconds = (long) Math.pow(2, retryCount) * 5;
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
```

---

## 🔒 Data Consistency Strategies

### **1. Eventual Consistency with Compensation**

#### **Order Processing Consistency Model**

```java
@Service
public class OrderConsistencyService {
    
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    
    @Transactional
    public void ensureOrderConsistency(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Check inventory consistency
        List<OrderItem> orderItems = order.getItems();
        for (OrderItem item : orderItems) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(item.getProductId()));
            
            // Verify inventory matches order
            if (inventory.getReservedQuantity() < item.getQuantity()) {
                log.warn("Inventory inconsistency detected for product: {}", item.getProductId());
                
                // Compensate by adjusting inventory
                inventory.adjustReservedQuantity(item.getQuantity() - inventory.getReservedQuantity());
                inventoryRepository.save(inventory);
            }
        }
        
        // Check payment consistency
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException(orderId));
        
        if (order.getStatus() == OrderStatus.CONFIRMED && 
            payment.getStatus() != PaymentStatus.AUTHORIZED) {
            log.error("Payment inconsistency detected for order: {}", orderId);
            
            // Compensate by cancelling order
            order.cancel("Payment inconsistency detected");
            orderRepository.save(order);
        }
    }
}
```

### **2. Idempotency for Critical Operations**

#### **Idempotency Key Implementation**

```java
@Entity
@Table(name = "idem_keys")
public class IdempotencyKey {
    @Id
    private String requestId;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "operation_type", nullable = false)
    private String operationType;
    
    @Column(name = "request_hash", nullable = false)
    private String requestHash;
    
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
    
    @Column(name = "http_status")
    private Integer httpStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}

@Service
public class IdempotencyService {
    
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    
    public boolean isDuplicate(String idempotencyKey) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
            .findById(idempotencyKey);
        
        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();
            
            // Check if key has expired
            if (key.getExpiresAt().isBefore(LocalDateTime.now())) {
                idempotencyKeyRepository.deleteById(idempotencyKey);
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    public <T> T getCachedResponse(String idempotencyKey, Class<T> responseType) {
        IdempotencyKey key = idempotencyKeyRepository.findById(idempotencyKey)
            .orElseThrow(() -> new IdempotencyKeyNotFoundException(idempotencyKey));
        
        try {
            return objectMapper.readValue(key.getResponseData(), responseType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cached response", e);
        }
    }
    
    public <T> void cacheResponse(String idempotencyKey, T response, UUID orderId) {
        try {
            IdempotencyKey key = IdempotencyKey.builder()
                .requestId(idempotencyKey)
                .orderId(orderId)
                .operationType("CREATE_ORDER")
                .requestHash("") // Could hash the request for additional validation
                .responseData(objectMapper.writeValueAsString(response))
                .httpStatus(200)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
            
            idempotencyKeyRepository.save(key);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response for caching", e);
        }
    }
}
```

---

## 📊 Event Schema Design

### **1. Event Schema Registry**

#### **Order Events Schema**

```json
{
  "OrderCreated": {
    "eventType": "OrderCreated",
    "version": "1.0",
    "schema": {
      "type": "object",
      "properties": {
        "orderId": {
          "type": "string",
          "format": "uuid",
          "description": "Unique identifier for the order"
        },
        "userId": {
          "type": "string",
          "format": "uuid",
          "description": "User who created the order"
        },
        "items": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "productId": {
                "type": "string",
                "format": "uuid"
              },
              "quantity": {
                "type": "integer",
                "minimum": 1
              },
              "unitPrice": {
                "type": "number",
                "minimum": 0
              }
            },
            "required": ["productId", "quantity", "unitPrice"]
          }
        },
        "totalAmount": {
          "type": "number",
          "minimum": 0
        },
        "createdAt": {
          "type": "string",
          "format": "date-time"
        }
      },
      "required": ["orderId", "userId", "items", "totalAmount", "createdAt"]
    }
  },
  "OrderConfirmed": {
    "eventType": "OrderConfirmed",
    "version": "1.0",
    "schema": {
      "type": "object",
      "properties": {
        "orderId": {
          "type": "string",
          "format": "uuid"
        },
        "confirmedAt": {
          "type": "string",
          "format": "date-time"
        },
        "paymentId": {
          "type": "string",
          "format": "uuid"
        }
      },
      "required": ["orderId", "confirmedAt", "paymentId"]
    }
  }
}
```

### **2. Event Validation Service**

```java
@Service
public class EventValidationService {
    
    private final Map<String, JsonSchema> eventSchemas;
    private final JsonSchemaFactory schemaFactory;
    
    public EventValidationService() {
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        this.eventSchemas = loadEventSchemas();
    }
    
    public ValidationResult validateEvent(String eventType, String eventPayload) {
        JsonSchema schema = eventSchemas.get(eventType);
        if (schema == null) {
            return ValidationResult.builder()
                .valid(false)
                .errors(List.of("Unknown event type: " + eventType))
                .build();
        }
        
        try {
            JsonNode eventNode = objectMapper.readTree(eventPayload);
            Set<ValidationMessage> validationMessages = schema.validate(eventNode);
            
            if (validationMessages.isEmpty()) {
                return ValidationResult.builder()
                    .valid(true)
                    .build();
            } else {
                List<String> errors = validationMessages.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());
                
                return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .build();
            }
            
        } catch (JsonProcessingException e) {
            return ValidationResult.builder()
                .valid(false)
                .errors(List.of("Invalid JSON format: " + e.getMessage()))
                .build();
        }
    }
    
    private Map<String, JsonSchema> loadEventSchemas() {
        Map<String, JsonSchema> schemas = new HashMap<>();
        
        // Load schemas from resources
        schemas.put("OrderCreated", loadSchema("schemas/OrderCreated.json"));
        schemas.put("OrderConfirmed", loadSchema("schemas/OrderConfirmed.json"));
        schemas.put("PaymentAuthorized", loadSchema("schemas/PaymentAuthorized.json"));
        schemas.put("InventoryReserved", loadSchema("schemas/InventoryReserved.json"));
        
        return schemas;
    }
}
```

---

## 🚀 Performance & Scalability

### **1. Event Processing Optimization**

#### **Batch Event Processing**

```java
@Service
public class BatchEventProcessor {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Scheduled(fixedRate = 1000) // Process every second
    public void processBatchEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
            .findPendingEventsForBatchProcessing(100); // Process up to 100 events
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        // Group events by topic for batch publishing
        Map<String, List<OutboxEvent>> eventsByTopic = pendingEvents.stream()
            .collect(Collectors.groupingBy(OutboxEvent::getEventType));
        
        // Process each topic batch
        eventsByTopic.forEach((topic, events) -> {
            try {
                // Create batch message
                List<String> eventPayloads = events.stream()
                    .map(OutboxEvent::getPayload)
                    .collect(Collectors.toList());
                
                String batchPayload = objectMapper.writeValueAsString(eventPayloads);
                
                // Publish batch
                kafkaTemplate.send(topic, batchPayload);
                
                // Mark all events as sent
                events.forEach(event -> {
                    event.markAsSent();
                    outboxEventRepository.save(event);
                });
                
                log.info("Batch processed {} events for topic: {}", events.size(), topic);
                
            } catch (Exception e) {
                log.error("Failed to process batch for topic: {}", topic, e);
                
                // Mark events for retry
                events.forEach(event -> {
                    event.incrementRetryCount();
                    event.setStatus(OutboxEventStatus.RETRY);
                    outboxEventRepository.save(event);
                });
            }
        });
    }
}
```

### **2. Database Performance Optimization**

#### **Connection Pooling & Query Optimization**

```yaml
# Database configuration for high performance
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
        
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## ❓ Common Interview Questions

### **Q1: How do you ensure data consistency across microservices?**

**Answer**: Multi-strategy approach:
1. **Eventual Consistency**: Accept temporary inconsistency for better performance
2. **Saga Pattern**: Use compensation logic for distributed transactions
3. **Outbox Pattern**: Ensure reliable event publishing
4. **Idempotency**: Handle duplicate requests gracefully
5. **Event Validation**: Validate event schemas before processing

### **Q2: What happens if an event fails to be published?**

**Answer**: Comprehensive failure handling:
1. **Retry Logic**: Exponential backoff with configurable retry counts
2. **Dead Letter Queues**: Store failed events for manual investigation
3. **Compensation**: Rollback changes if downstream processing fails
4. **Monitoring**: Alert on repeated failures
5. **Manual Recovery**: Admin tools to reprocess failed events

### **Q3: How do you handle event ordering and duplicates?**

**Answer**: Event ordering and deduplication:
1. **Event Versioning**: Include version numbers in events
2. **Idempotency Keys**: Unique identifiers for deduplication
3. **Event Store**: Maintain event sequence for ordering
4. **Consumer Groups**: Ensure ordered processing per partition
5. **Watermarking**: Track processing progress

### **Q4: How do you scale event processing?**

**Answer**: Multi-level scaling:
1. **Kafka Partitioning**: Distribute events across partitions
2. **Consumer Scaling**: Multiple consumer instances
3. **Batch Processing**: Process events in batches for efficiency
4. **Async Processing**: Non-blocking event handling
5. **Caching**: Cache frequently accessed data

---

## 🎯 Data Consistency Assessment

### **Consistency Model: Eventual Consistency (9/10)**

| Aspect | Score | Implementation | Benefits |
|--------|-------|----------------|----------|
| **Data Integrity** | 9/10 | Saga + Compensation | Handles failures gracefully |
| **Performance** | 10/10 | Async + Batching | High throughput processing |
| **Reliability** | 9/10 | Outbox + Retry | Guaranteed event delivery |
| **Scalability** | 10/10 | Partitioning + Batching | Horizontal scaling |
| **Monitoring** | 8/10 | Metrics + Alerting | Visibility into system health |

### **Key Strengths**
1. **Reliable Event Delivery**: Outbox pattern ensures no events are lost
2. **Fault Tolerance**: Compensation logic handles partial failures
3. **High Performance**: Async processing with batching
4. **Scalability**: Kafka-based event streaming
5. **Data Validation**: Schema validation for all events

### **Areas for Enhancement**
1. **Event Sourcing**: Could implement full event sourcing for audit trails
2. **CQRS**: Could separate read and write models for better performance
3. **Event Versioning**: Could implement more sophisticated versioning

---

*This guide covers data consistency and event-driven architecture patterns. For detailed implementations, refer to the individual service logs.*
