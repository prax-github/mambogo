# CON-03: Event Schemas Interview Guide

**Task ID:** CON-03  
**Task Name:** Event schemas v1 + registry (JSON Schema)  
**Status:** ✅ Completed  
**Date:** 2025-08-29  
**Owner:** Prashant Sinha  

---

## 🎯 Interview Context

This guide covers the implementation of comprehensive JSON Schema definitions for domain events in the e-commerce system. The interviewer will assess your understanding of event-driven architecture, schema design, and event validation patterns.

---

## 📋 Key Topics to Master

### 1. JSON Schema Standards

#### Core Concepts
- **JSON Schema Draft-07**: Latest stable version with enhanced validation
- **Event Envelope Pattern**: Consistent event structure across all events
- **Schema Registry**: Centralized event schema management
- **Schema Evolution**: Versioning and migration strategies

#### Standard Event Envelope Structure
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "EventName",
  "description": "Event description",
  "type": "object",
  "required": [
    "eventId",
    "eventType",
    "timestamp",
    "data"
  ],
  "properties": {
    "eventId": {
      "type": "string",
      "format": "uuid",
      "description": "Unique event identifier"
    },
    "eventType": {
      "type": "string",
      "const": "EventName",
      "description": "Event type"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "Event timestamp in ISO 8601 format"
    },
    "data": {
      "type": "object",
      "description": "Event-specific data payload"
    }
  }
}
```

### 2. Domain Event Schemas

#### Order Domain Events

##### OrderCreated Event
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "OrderCreated",
  "description": "Event emitted when a new order is created",
  "type": "object",
  "required": [
    "eventId",
    "eventType",
    "timestamp",
    "data"
  ],
  "properties": {
    "eventId": {
      "type": "string",
      "format": "uuid",
      "description": "Unique event identifier"
    },
    "eventType": {
      "type": "string",
      "const": "OrderCreated",
      "description": "Event type"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "Event timestamp in ISO 8601 format"
    },
    "data": {
      "type": "object",
      "required": [
        "orderId",
        "userId",
        "items",
        "totalAmount",
        "shippingAddress"
      ],
      "properties": {
        "orderId": {
          "type": "string",
          "format": "uuid",
          "description": "Order ID"
        },
        "userId": {
          "type": "string",
          "format": "uuid",
          "description": "User ID"
        },
        "items": {
          "type": "array",
          "items": {
            "type": "object",
            "required": [
              "productId",
              "productName",
              "price",
              "quantity"
            ],
            "properties": {
              "productId": {
                "type": "string",
                "format": "uuid",
                "description": "Product ID"
              },
              "productName": {
                "type": "string",
                "description": "Product name"
              },
              "price": {
                "type": "number",
                "minimum": 0,
                "description": "Product price at time of order"
              },
              "quantity": {
                "type": "integer",
                "minimum": 1,
                "description": "Item quantity"
              }
            }
          }
        },
        "totalAmount": {
          "type": "number",
          "minimum": 0,
          "description": "Total order amount"
        }
      }
    }
  }
}
```

#### Payment Domain Events

##### PaymentAuthorized Event
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PaymentAuthorized",
  "description": "Event emitted when a payment is authorized",
  "type": "object",
  "required": [
    "eventId",
    "eventType",
    "timestamp",
    "data"
  ],
  "properties": {
    "eventId": {
      "type": "string",
      "format": "uuid",
      "description": "Unique event identifier"
    },
    "eventType": {
      "type": "string",
      "const": "PaymentAuthorized",
      "description": "Event type"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "Event timestamp in ISO 8601 format"
    },
    "data": {
      "type": "object",
      "required": [
        "paymentId",
        "orderId",
        "userId",
        "amount",
        "currency",
        "transactionId",
        "paymentMethod"
      ],
      "properties": {
        "paymentId": {
          "type": "string",
          "format": "uuid",
          "description": "Payment ID"
        },
        "orderId": {
          "type": "string",
          "format": "uuid",
          "description": "Order ID"
        },
        "amount": {
          "type": "number",
          "minimum": 0,
          "description": "Payment amount"
        },
        "currency": {
          "type": "string",
          "default": "USD",
          "description": "Payment currency"
        }
      }
    }
  }
}
```

### 3. Schema Registry Implementation

#### Event Schema Registry
```java
@Component
public class EventSchemaRegistry {
    
    private final Map<String, JsonSchema> schemas = new HashMap<>();
    private final ObjectMapper objectMapper;
    
    public EventSchemaRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadSchemas();
    }
    
    private void loadSchemas() {
        // Load JSON schemas from classpath
        loadSchema("order-created");
        loadSchema("payment-authorized");
        loadSchema("payment-failed");
    }
    
    public JsonSchema getSchema(String eventType) {
        return schemas.get(eventType);
    }
    
    public boolean validateEvent(String eventType, String eventJson) {
        JsonSchema schema = getSchema(eventType);
        if (schema == null) {
            throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
        
        try {
            JsonNode eventNode = objectMapper.readTree(eventJson);
            return schema.validate(eventNode).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 4. Schema Validation Integration

#### Schema-Validated Event Publisher
```java
@Component
public class SchemaValidatedEventPublisher {
    
    private final EventSchemaRegistry schemaRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public void publishEvent(String eventType, Object eventData) {
        try {
            // Create event envelope
            EventEnvelope envelope = createEventEnvelope(eventType, eventData);
            String eventJson = objectMapper.writeValueAsString(envelope);
            
            // Validate against schema
            if (!schemaRegistry.validateEvent(eventType, eventJson)) {
                throw new EventValidationException("Event validation failed for type: " + eventType);
            }
            
            // Publish to Kafka
            kafkaTemplate.send(eventType + ".events", eventJson);
            
        } catch (Exception e) {
            throw new EventPublishException("Failed to publish event", e);
        }
    }
}
```

---

## 🧠 Deep Dive Questions

### 1. Schema Design Principles

**Q: Why did you choose JSON Schema Draft-07 over other schema standards?**
**A:** JSON Schema Draft-07 provides several advantages:
- **Industry Standard**: Widely adopted and supported
- **Rich Validation**: Comprehensive validation capabilities
- **Tooling Support**: Excellent tooling and library support
- **Performance**: Efficient validation performance
- **Extensibility**: Easy to extend with custom validators

**Q: How do you ensure schema consistency across different event types?**
**A:** Schema consistency is achieved through:
- **Common Envelope**: All events use the same envelope structure
- **Shared Schemas**: Reuse common schema definitions
- **Validation Rules**: Consistent validation patterns
- **Documentation Standards**: Uniform documentation approach
- **Tooling**: Automated schema validation tools

### 2. Schema Evolution and Versioning

**Q: How do you handle schema evolution in an event-driven system?**
**A:** Schema evolution strategy includes:
- **Backward Compatibility**: New fields must be optional
- **Versioning Strategy**: Clear versioning approach
- **Migration Tools**: Automated migration utilities
- **Deprecation Process**: Clear deprecation guidelines
- **Testing**: Comprehensive schema testing

**Q: How do you handle breaking changes in event schemas?**
**A:** Breaking change management:
- **Gradual Migration**: Migrate consumers gradually
- **Dual Publishing**: Publish both old and new formats
- **Consumer Coordination**: Coordinate with all consumers
- **Rollback Strategy**: Ability to rollback if issues arise
- **Monitoring**: Monitor migration progress

### 3. Event Validation and Processing

**Q: How do you validate events at runtime?**
**A:** Runtime validation implementation:
- **Schema Registry**: Centralized schema management
- **Validation Pipeline**: Validate events before processing
- **Error Handling**: Comprehensive error handling for invalid events
- **Dead Letter Queues**: Route invalid events to DLQ
- **Monitoring**: Monitor validation success rates

**Q: How do you handle schema validation performance?**
**A:** Performance optimization strategies:
- **Schema Caching**: Cache compiled schemas
- **Lazy Validation**: Validate only when necessary
- **Parallel Processing**: Validate events in parallel
- **Resource Limits**: Limit validation resource usage
- **Monitoring**: Track validation performance metrics

### 4. Event Registry Management

**Q: How do you manage the event schema registry?**
**A:** Registry management includes:
- **Centralized Storage**: Store schemas in a central location
- **Version Control**: Track schema versions in Git
- **Automated Deployment**: Deploy schemas with applications
- **Schema Discovery**: Automatic schema discovery
- **Documentation**: Comprehensive schema documentation

**Q: How do you ensure schema registry availability?**
**A:** Availability strategies:
- **Redundancy**: Multiple registry instances
- **Caching**: Cache schemas locally
- **Fallback Mechanisms**: Fallback to local schemas
- **Health Checks**: Monitor registry health
- **Alerting**: Alert on registry issues

---

## 🔧 Technical Implementation Questions

### 1. Schema Loading and Caching

**Q: How do you load and cache schemas efficiently?**
**A:** Schema loading strategy:
- **Classpath Loading**: Load schemas from classpath resources
- **Lazy Loading**: Load schemas on first use
- **Caching**: Cache compiled schemas in memory
- **Hot Reloading**: Support schema hot reloading in development
- **Validation**: Validate schemas on load

**Q: How do you handle schema compilation and optimization?**
**A:** Compilation optimization:
- **Pre-compilation**: Compile schemas at build time
- **Optimization**: Optimize schema validation performance
- **Memory Management**: Efficient memory usage
- **Garbage Collection**: Proper garbage collection
- **Monitoring**: Monitor compilation performance

### 2. Event Processing Integration

**Q: How do you integrate schema validation with event processing?**
**A:** Integration approach:
- **Validation Pipeline**: Validate events in processing pipeline
- **Error Handling**: Handle validation errors gracefully
- **Performance Impact**: Minimize validation performance impact
- **Monitoring**: Monitor validation metrics
- **Testing**: Comprehensive integration testing

**Q: How do you handle schema validation errors?**
**A:** Error handling strategy:
- **Error Classification**: Classify validation errors
- **Error Reporting**: Report validation errors clearly
- **Dead Letter Queues**: Route invalid events to DLQ
- **Retry Logic**: Retry validation if appropriate
- **Monitoring**: Monitor validation error patterns

### 3. Schema Testing and Quality

**Q: How do you test event schemas comprehensively?**
**A:** Schema testing strategy:
- **Unit Testing**: Test individual schema validation
- **Integration Testing**: Test schema validation in context
- **Contract Testing**: Verify schema compliance
- **Performance Testing**: Test schema validation performance
- **Security Testing**: Test schema security aspects

**Q: How do you ensure schema quality and consistency?**
**A:** Quality assurance:
- **Automated Validation**: Validate schemas automatically
- **Code Review**: Review schema changes
- **Documentation**: Comprehensive schema documentation
- **Examples**: Provide realistic schema examples
- **Standards**: Follow schema design standards

---

## 🎯 System Design Questions

### 1. Event-Driven Architecture

**Q: How do events fit into your overall system architecture?**
**A:** Event architecture integration:
- **Event Sourcing**: Use events for state reconstruction
- **CQRS**: Separate command and query responsibilities
- **Saga Pattern**: Use events for saga orchestration
- **Event Streaming**: Stream events for real-time processing
- **Analytics**: Use events for analytics and reporting

**Q: How do you handle event ordering and consistency?**
**A:** Ordering and consistency:
- **Partition Keys**: Use partition keys for ordering
- **Event Versioning**: Version events for consistency
- **Idempotency**: Ensure idempotent event processing
- **Causality**: Track event causality
- **Monitoring**: Monitor event ordering

### 2. Schema Registry Architecture

**Q: How do you design a scalable schema registry?**
**A:** Scalable registry design:
- **Distributed Storage**: Use distributed storage for schemas
- **Caching Layer**: Implement caching for performance
- **Load Balancing**: Balance load across registry instances
- **Auto-scaling**: Auto-scale based on demand
- **Monitoring**: Comprehensive monitoring and alerting

**Q: How do you handle schema registry failures?**
**A:** Failure handling:
- **Circuit Breakers**: Use circuit breakers for registry calls
- **Fallback Mechanisms**: Fallback to local schemas
- **Retry Logic**: Implement intelligent retry
- **Monitoring**: Monitor registry health
- **Alerting**: Alert on registry failures

---

## 📊 Metrics and Monitoring

### 1. Schema Quality Metrics

**Key Metrics to Track:**
- **Schema Validation Success Rate**: Percentage of events that pass validation
- **Schema Compilation Time**: Time to compile schemas
- **Schema Cache Hit Rate**: Cache hit rate for schemas
- **Schema Registry Availability**: Registry uptime percentage
- **Schema Version Distribution**: Distribution of schema versions

### 2. Event Processing Metrics

**Processing Indicators:**
- **Event Validation Latency**: Time to validate events
- **Invalid Event Rate**: Rate of invalid events
- **Schema Registry Response Time**: Registry response times
- **Event Processing Throughput**: Events processed per second
- **Error Rate**: Rate of schema-related errors

---

## 🚀 Production Considerations

### 1. Schema Management in Production

**Production Requirements:**
- **Schema Versioning**: Proper schema versioning strategy
- **Backward Compatibility**: Maintain backward compatibility
- **Rollback Capability**: Ability to rollback schema changes
- **Monitoring**: Comprehensive schema monitoring
- **Documentation**: Up-to-date schema documentation

### 2. Schema Evolution Strategy

**Evolution Process:**
- **Change Management**: Proper change management process
- **Testing**: Comprehensive testing of schema changes
- **Deployment**: Safe deployment of schema changes
- **Monitoring**: Monitor impact of schema changes
- **Rollback**: Ability to rollback if issues arise

---

## 📝 Best Practices Summary

### 1. Schema Design Principles
- **Consistency**: Use consistent schema patterns
- **Validation**: Comprehensive validation rules
- **Documentation**: Clear schema documentation
- **Examples**: Provide realistic examples
- **Extensibility**: Design for future enhancements

### 2. Schema Management Standards
- **Version Control**: Track schema versions in Git
- **Automated Validation**: Validate schemas automatically
- **Code Review**: Review schema changes
- **Testing**: Comprehensive schema testing
- **Documentation**: Maintain schema documentation

### 3. Performance Guidelines
- **Caching**: Cache schemas for performance
- **Optimization**: Optimize schema validation
- **Monitoring**: Monitor schema performance
- **Resource Management**: Efficient resource usage
- **Scalability**: Design for scalability

---

## 🎯 Interview Success Tips

### 1. Preparation
- **Review Your Implementation**: Understand every aspect of your schema design
- **Practice Examples**: Be ready to walk through specific schema examples
- **Understand Trade-offs**: Know the pros/cons of your design decisions
- **Prepare Metrics**: Have schema quality metrics ready

### 2. Communication
- **Start High-Level**: Begin with schema architecture overview
- **Provide Examples**: Use concrete examples to illustrate points
- **Explain Rationale**: Justify your schema design decisions
- **Acknowledge Limitations**: Be honest about trade-offs and limitations

### 3. Problem-Solving
- **Think Aloud**: Explain your thought process for schema design
- **Consider Alternatives**: Discuss different schema approaches
- **Ask Clarifying Questions**: Ensure you understand the requirements
- **Propose Solutions**: Offer concrete solutions to schema problems

---

## 📚 Additional Resources

### Documentation
- [JSON Schema Specification](https://json-schema.org/)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)

### Tools
- [JSON Schema Validator](https://json-schema.org/implementations.html)
- [Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Kafka Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)

### Standards
- [JSON Schema Draft-07](https://json-schema.org/specification.html)
- [CloudEvents](https://cloudevents.io/) - Event specification
- [AsyncAPI](https://www.asyncapi.com/) - Async API specification

---

**Remember:** The key to success is demonstrating deep understanding of event-driven architecture, schema design principles, and practical implementation experience with JSON Schema validation.
