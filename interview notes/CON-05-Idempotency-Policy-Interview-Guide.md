# CON-05: Idempotency Policy Interview Guide

**Task ID:** CON-05  
**Task Name:** Idempotency policy (header, storage, replay)  
**Status:** ✅ Completed  
**Date:** 2025-08-29  
**Owner:** Prashant Sinha  

---

## 🎯 Interview Context

This guide covers the implementation of a comprehensive idempotency policy across the e-commerce system to prevent duplicate operations, ensure data consistency, and handle retry scenarios gracefully. The interviewer will assess your understanding of distributed systems, data consistency, and fault tolerance patterns.

---

## 📋 Key Topics to Master

### 1. Idempotency Key Header Implementation

#### HTTP Header Specification
```http
Idempotency-Key: {unique-key}
```

#### Header Requirements
- **Format**: String, maximum 64 characters
- **Uniqueness**: Must be unique per operation
- **Validity**: 24-hour window for key reuse
- **Generation**: Client-generated or server-generated
- **Encoding**: Base64-safe characters only

#### Header Validation
```java
@Component
public class IdempotencyKeyValidator {
    
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = 
        Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    
    public boolean isValidIdempotencyKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        return IDEMPOTENCY_KEY_PATTERN.matcher(key.trim()).matches();
    }
    
    public void validateIdempotencyKey(String key) {
        if (!isValidIdempotencyKey(key)) {
            throw new InvalidIdempotencyKeyException(
                "Invalid idempotency key format. Must be 1-64 characters, " +
                "containing only letters, numbers, hyphens, and underscores."
            );
        }
    }
}
```

### 2. Idempotency Storage Implementation

#### Database Schema
```sql
CREATE TABLE idem_keys (
    request_id VARCHAR(64) PRIMARY KEY,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    request_hash VARCHAR(64),
    response_data JSONB,
    http_status INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_operation_type (operation_type)
);
```

#### Idempotency Key Entity
```java
@Entity
@Table(name = "idem_keys")
public class IdempotencyKey {
    
    @Id
    @Column(name = "request_id", length = 64)
    private String requestId;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;
    
    @Column(name = "request_hash", length = 64)
    private String requestHash;
    
    @Column(name = "response_data", columnDefinition = "JSONB")
    private String responseData;
    
    @Column(name = "http_status")
    private Integer httpStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
```

### 3. Idempotency Service Implementation

#### Core Idempotency Service
```java
@Service
@Transactional
public class IdempotencyService {
    
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final IdempotencyKeyValidator idempotencyKeyValidator;
    private final ObjectMapper objectMapper;
    
    public IdempotencyResponse checkIdempotency(String idempotencyKey, 
                                              UUID userId, 
                                              String operationType) {
        
        // Validate idempotency key format
        idempotencyKeyValidator.validateIdempotencyKey(idempotencyKey);
        
        // Check for existing key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
            .findByRequestIdAndExpiresAtAfter(idempotencyKey, LocalDateTime.now());
        
        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            
            // Verify user ownership
            if (!key.getUserId().equals(userId)) {
                throw new IdempotencyKeyConflictException(
                    "Idempotency key already used by another user"
                );
            }
            
            // Return cached response
            return IdempotencyResponse.builder()
                .isDuplicate(true)
                .cachedResponse(key.getResponseData())
                .httpStatus(key.getHttpStatus())
                .originalOrderId(key.getOrderId())
                .build();
        }
        
        return IdempotencyResponse.builder()
            .isDuplicate(false)
            .build();
    }
    
    public void storeIdempotencyKey(String idempotencyKey, 
                                  UUID orderId, 
                                  UUID userId, 
                                  String operationType, 
                                  String responseData, 
                                  int httpStatus) {
        
        IdempotencyKey key = new IdempotencyKey();
        key.setRequestId(idempotencyKey);
        key.setOrderId(orderId);
        key.setUserId(userId);
        key.setOperationType(operationType);
        key.setResponseData(responseData);
        key.setHttpStatus(httpStatus);
        key.setCreatedAt(LocalDateTime.now());
        key.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        idempotencyKeyRepository.save(key);
    }
}
```

### 4. Controller Integration

#### Order Controller with Idempotency
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = extractUserIdFromJwt(jwt);
        
        // Check idempotency
        IdempotencyResponse idempotencyResponse = idempotencyService
            .checkIdempotency(idempotencyKey, userId, "CREATE_ORDER");
        
        if (idempotencyResponse.isDuplicate()) {
            // Return cached response
            OrderResponse cachedResponse = objectMapper.readValue(
                idempotencyResponse.getCachedResponse(), 
                OrderResponse.class
            );
            
            return ResponseEntity.status(idempotencyResponse.getHttpStatus())
                .body(cachedResponse);
        }
        
        // Process new order
        try {
            OrderResponse response = orderService.createOrder(request, userId);
            
            // Store idempotency key
            idempotencyService.storeIdempotencyKey(
                idempotencyKey,
                response.getData().getId(),
                userId,
                "CREATE_ORDER",
                objectMapper.writeValueAsString(response),
                HttpStatus.CREATED.value()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            // Store failed response for idempotency
            ErrorResponse errorResponse = createErrorResponse(e);
            idempotencyService.storeIdempotencyKey(
                idempotencyKey,
                null,
                userId,
                "CREATE_ORDER",
                objectMapper.writeValueAsString(errorResponse),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            
            throw e;
        }
    }
}
```

---

## 🧠 Deep Dive Questions

### 1. Idempotency Principles

**Q: Why is idempotency important in distributed systems?**
**A:** Idempotency is crucial because:
- **Network Failures**: Retries can cause duplicate operations
- **Client Failures**: Client crashes can lead to retries
- **Load Balancers**: Load balancers may retry failed requests
- **Data Consistency**: Prevents duplicate data creation
- **User Experience**: Provides predictable behavior

**Q: How do you ensure idempotency across multiple services?**
**A:** Cross-service idempotency:
- **Distributed Keys**: Use globally unique keys
- **Event Sourcing**: Use events for state reconstruction
- **Saga Pattern**: Use sagas for distributed transactions
- **Outbox Pattern**: Ensure reliable message delivery
- **Monitoring**: Monitor idempotency across services

### 2. Key Generation and Management

**Q: How do you generate unique idempotency keys?**
**A:** Key generation strategies:
- **Client-Generated**: Client generates keys for control
- **Server-Generated**: Server generates keys for consistency
- **UUID-Based**: Use UUIDs for uniqueness
- **Time-Based**: Include timestamps for ordering
- **Hash-Based**: Hash request content for uniqueness

**Q: How do you handle key collisions?**
**A:** Collision handling:
- **User Scoping**: Scope keys by user ID
- **Operation Scoping**: Scope keys by operation type
- **Time Windows**: Use time-based windows
- **Conflict Detection**: Detect and handle conflicts
- **Monitoring**: Monitor collision rates

### 3. Storage and Performance

**Q: How do you optimize idempotency storage performance?**
**A:** Performance optimization:
- **Database Indexing**: Index key fields for fast lookups
- **Caching**: Cache frequently accessed keys
- **Partitioning**: Partition storage by user or time
- **Cleanup**: Regular cleanup of expired keys
- **Monitoring**: Monitor storage performance

**Q: How do you handle storage failures?**
**A:** Failure handling:
- **Circuit Breakers**: Use circuit breakers for storage
- **Fallback Mechanisms**: Fallback to local storage
- **Retry Logic**: Implement intelligent retry
- **Monitoring**: Monitor storage health
- **Alerting**: Alert on storage failures

### 4. Response Replay and Consistency

**Q: How do you ensure response consistency in idempotency?**
**A:** Consistency strategies:
- **Request Hashing**: Hash request content
- **Response Caching**: Cache complete responses
- **State Tracking**: Track operation state
- **Validation**: Validate request consistency
- **Monitoring**: Monitor consistency metrics

**Q: How do you handle partial failures in idempotent operations?**
**A:** Partial failure handling:
- **Transaction Management**: Use database transactions
- **Compensation Logic**: Implement compensation
- **State Management**: Track operation state
- **Rollback**: Rollback on partial failures
- **Monitoring**: Monitor failure patterns

---

## 🔧 Technical Implementation Questions

### 1. Database Design and Optimization

**Q: How do you design the idempotency database schema?**
**A:** Schema design considerations:
- **Primary Key**: Use request ID as primary key
- **Indexes**: Index user ID, expiration time, operation type
- **Data Types**: Use appropriate data types
- **Constraints**: Add appropriate constraints
- **Partitioning**: Consider partitioning for large datasets

**Q: How do you handle database scaling for idempotency?**
**A:** Scaling strategies:
- **Horizontal Scaling**: Use read replicas
- **Partitioning**: Partition by user or time
- **Caching**: Use distributed caching
- **Sharding**: Shard by user ID
- **Monitoring**: Monitor scaling metrics

### 2. Security and Privacy

**Q: How do you secure idempotency keys?**
**A:** Security measures:
- **Key Validation**: Validate key format and content
- **User Scoping**: Scope keys by user
- **Access Control**: Control access to keys
- **Audit Logging**: Log key access
- **Encryption**: Encrypt sensitive data

**Q: How do you handle sensitive data in idempotency storage?**
**A:** Sensitive data handling:
- **Data Masking**: Mask sensitive fields
- **Encryption**: Encrypt sensitive data
- **Access Control**: Limit access to sensitive data
- **Audit Trails**: Maintain audit trails
- **Compliance**: Ensure compliance requirements

### 3. Integration and Testing

**Q: How do you test idempotency comprehensively?**
**A:** Testing strategy:
- **Unit Testing**: Test individual components
- **Integration Testing**: Test end-to-end flows
- **Load Testing**: Test under high load
- **Failure Testing**: Test failure scenarios
- **Security Testing**: Test security aspects

**Q: How do you integrate idempotency with existing systems?**
**A:** Integration approach:
- **Middleware**: Use middleware for integration
- **AOP**: Use aspect-oriented programming
- **Decorators**: Use decorator pattern
- **Configuration**: Make idempotency configurable
- **Monitoring**: Monitor integration metrics

---

## 🎯 System Design Questions

### 1. Distributed System Design

**Q: How do you design idempotency for a distributed system?**
**A:** Distributed design:
- **Distributed Keys**: Use globally unique keys
- **Event Sourcing**: Use events for state
- **CQRS**: Separate command and query
- **Saga Pattern**: Use sagas for transactions
- **Monitoring**: Monitor distributed state

**Q: How do you handle idempotency in microservices?**
**A:** Microservices approach:
- **Service-Specific Keys**: Use service-specific keys
- **Event Correlation**: Correlate events across services
- **State Management**: Manage state per service
- **API Gateway**: Handle at API gateway level
- **Monitoring**: Monitor across services

### 2. Performance and Scalability

**Q: How do you optimize idempotency for high throughput?**
**A:** Throughput optimization:
- **Caching**: Cache frequently accessed keys
- **Async Processing**: Process asynchronously
- **Batch Operations**: Use batch operations
- **Connection Pooling**: Use connection pooling
- **Monitoring**: Monitor performance metrics

**Q: How do you handle idempotency at scale?**
**A:** Scaling strategies:
- **Horizontal Scaling**: Scale horizontally
- **Caching**: Use distributed caching
- **Partitioning**: Partition data
- **Load Balancing**: Balance load
- **Monitoring**: Monitor scaling metrics

---

## 📊 Metrics and Monitoring

### 1. Idempotency Metrics

**Key Metrics to Track:**
- **Key Creation Rate**: Rate of key creation
- **Duplicate Detection Rate**: Rate of duplicate detection
- **Storage Usage**: Storage usage for keys
- **Response Time**: Time to check idempotency
- **Error Rate**: Rate of idempotency errors

### 2. Performance Metrics

**Performance Indicators:**
- **Cache Hit Rate**: Cache hit rate for keys
- **Database Performance**: Database performance metrics
- **Response Latency**: Latency impact of idempotency
- **Throughput**: Throughput with idempotency
- **Resource Usage**: Resource usage for idempotency

---

## 🚀 Production Considerations

### 1. Production Deployment

**Production Requirements:**
- **High Availability**: Ensure high availability
- **Performance**: Meet performance requirements
- **Security**: Implement security measures
- **Monitoring**: Comprehensive monitoring
- **Documentation**: Complete documentation

### 2. Maintenance and Operations

**Operational Procedures:**
- **Key Cleanup**: Regular key cleanup
- **Performance Monitoring**: Monitor performance
- **Capacity Planning**: Plan for capacity
- **Incident Response**: Handle incidents
- **Change Management**: Manage changes

---

## 📝 Best Practices Summary

### 1. Idempotency Design Principles
- **Uniqueness**: Ensure key uniqueness
- **Consistency**: Maintain response consistency
- **Performance**: Optimize for performance
- **Security**: Implement security measures
- **Monitoring**: Comprehensive monitoring

### 2. Implementation Guidelines
- **Key Generation**: Use appropriate key generation
- **Storage Design**: Design storage for performance
- **Error Handling**: Handle errors gracefully
- **Testing**: Comprehensive testing
- **Documentation**: Document implementation

### 3. Operational Standards
- **Monitoring**: Monitor operational metrics
- **Alerting**: Set up appropriate alerting
- **Maintenance**: Regular maintenance procedures
- **Documentation**: Maintain documentation
- **Training**: Train operational staff

---

## 🎯 Interview Success Tips

### 1. Preparation
- **Review Your Implementation**: Understand every aspect of your idempotency design
- **Practice Examples**: Be ready to walk through specific scenarios
- **Understand Trade-offs**: Know the pros/cons of your design decisions
- **Prepare Metrics**: Have performance metrics ready

### 2. Communication
- **Start High-Level**: Begin with idempotency architecture overview
- **Provide Examples**: Use concrete examples to illustrate points
- **Explain Rationale**: Justify your design decisions
- **Acknowledge Limitations**: Be honest about trade-offs and limitations

### 3. Problem-Solving
- **Think Aloud**: Explain your thought process for idempotency design
- **Consider Alternatives**: Discuss different design approaches
- **Ask Clarifying Questions**: Ensure you understand the requirements
- **Propose Solutions**: Offer concrete solutions to design problems

---

## 📚 Additional Resources

### Documentation
- [HTTP Idempotency](https://tools.ietf.org/html/rfc7231#section-4.2.2)
- [Distributed Systems](https://en.wikipedia.org/wiki/Distributed_computing)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)

### Tools
- [Spring Boot](https://spring.io/projects/spring-boot) for implementation
- [PostgreSQL](https://www.postgresql.org/) for storage
- [Redis](https://redis.io/) for caching

### Standards
- [HTTP Idempotency](https://tools.ietf.org/html/rfc7231#section-4.2.2)
- [JSON Web Tokens](https://tools.ietf.org/html/rfc7519)
- [UUID](https://tools.ietf.org/html/rfc4122)

---

**Remember:** The key to success is demonstrating deep understanding of distributed systems, data consistency patterns, and practical implementation experience with idempotency mechanisms.
