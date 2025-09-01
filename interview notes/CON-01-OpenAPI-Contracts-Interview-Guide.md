# CON-01: OpenAPI Contracts Interview Guide

**Task ID:** CON-01  
**Task Name:** OpenAPI v1 per service (commit under `docs/contracts/api`)  
**Status:** ✅ Completed  
**Date:** 2025-08-29  
**Owner:** Prashant Sinha  

---

## 🎯 Interview Context

This guide covers the implementation of comprehensive OpenAPI v3.0.3 specifications for all microservices in the e-commerce system. The interviewer will assess your understanding of API design principles, contract-first development, and microservices integration.

---

## 📋 Key Topics to Master

### 1. OpenAPI Specification Standards

#### Core Concepts
- **OpenAPI v3.0.3**: Latest stable version with enhanced features
- **Contract-First Development**: Define APIs before implementation
- **Schema Validation**: Comprehensive request/response validation
- **Documentation**: Self-documenting APIs with examples

#### Specification Structure
```yaml
openapi: 3.0.3
info:
  title: Service Name API
  description: API description
  version: 1.0.0
servers:
  - url: http://localhost:8080
paths:
  /endpoint:
    get:
      summary: Endpoint description
      responses:
        '200':
          description: Success response
components:
  schemas:
    Model:
      type: object
      properties:
        field: {type: string}
```

### 2. Service-Specific API Design

#### Product Service API
**Endpoints:**
- `GET /products` - List products with pagination and filtering
- `GET /products/{id}` - Get product by ID

**Key Features:**
- Pagination support (page, size parameters)
- Category filtering
- Comprehensive product schema
- Public access (no authentication required)

#### Cart Service API
**Endpoints:**
- `GET /cart/{userId}` - Get user's cart
- `POST /cart/{userId}/items` - Add item to cart
- `PUT /cart/{userId}/items/{itemId}` - Update cart item
- `DELETE /cart/{userId}/items/{itemId}` - Remove item from cart

**Key Features:**
- User-scoped operations
- JWT authentication required
- Cart item validation
- Real-time cart updates

#### Order Service API
**Endpoints:**
- `POST /orders` - Create new order (with idempotency)
- `GET /orders` - Get user's orders
- `GET /orders/{id}` - Get order by ID
- `PUT /orders/{id}/cancel` - Cancel order

**Key Features:**
- Idempotency key header required
- Order lifecycle management
- Comprehensive order schema
- Business rule validation

#### Payment Service API
**Endpoints:**
- `POST /payments` - Process payment
- `GET /payments` - Get user's payments
- `GET /payments/{id}` - Get payment by ID
- `POST /payments/{id}/refund` - Process refund

**Key Features:**
- Payment method validation
- Transaction tracking
- Refund processing
- Security compliance

### 3. Authentication and Authorization

#### JWT Integration
```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
paths:
  /secured-endpoint:
    get:
      security:
        - bearerAuth: []
```

#### Scope-Based Authorization
- `product:read` - Product catalog access
- `cart:manage` - Cart operations
- `order:write` - Order creation and management
- `payment:write` - Payment processing

### 4. Error Handling Standards

#### Standardized Error Response
```yaml
components:
  schemas:
    ErrorResponse:
      type: object
      required:
        - code
        - message
        - traceId
      properties:
        code:
          type: string
          description: Error code
        message:
          type: string
          description: Error message
        traceId:
          type: string
          description: Request trace ID
        timestamp:
          type: string
          format: date-time
```

#### HTTP Status Codes
- `200` - Success
- `201` - Created (for POST operations)
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (missing/invalid JWT)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `409` - Conflict (idempotency, duplicate resources)
- `422` - Unprocessable Entity (business rule violations)
- `500` - Internal Server Error

---

## 🧠 Deep Dive Questions

### 1. API Design Principles

**Q: Why did you choose OpenAPI v3.0.3 over other versions?**
**A:** OpenAPI v3.0.3 is the latest stable version with enhanced features:
- Better JSON Schema support
- Improved security scheme definitions
- Enhanced parameter validation
- Better documentation capabilities
- Industry standard adoption

**Q: How do you ensure API consistency across multiple services?**
**A:** Through several strategies:
- Shared schema definitions for common objects
- Standardized error response format
- Consistent naming conventions (kebab-case for paths, camelCase for properties)
- Common authentication patterns
- Unified documentation standards

### 2. Contract-First Development

**Q: What are the benefits of contract-first development?**
**A:** Contract-first development provides:
- **Early Validation**: Catch design issues before implementation
- **Parallel Development**: Frontend and backend can develop simultaneously
- **Code Generation**: Generate client SDKs and server stubs
- **Documentation**: Self-documenting APIs
- **Testing**: Contract testing ensures API compliance

**Q: How do you handle API versioning?**
**A:** API versioning strategy includes:
- **URL Versioning**: `/api/v1/orders`, `/api/v2/orders`
- **Header Versioning**: `Accept: application/vnd.api+json;version=1`
- **Backward Compatibility**: New fields are optional
- **Deprecation Strategy**: Clear migration path for breaking changes

### 3. Microservices Integration

**Q: How do you handle service discovery in your API contracts?**
**A:** Service discovery is handled through:
- **Gateway Proxy**: All APIs accessible through gateway
- **Service URLs**: Direct service URLs for internal communication
- **Load Balancing**: Gateway handles service routing
- **Health Checks**: Services expose health endpoints

**Q: How do you ensure API security across services?**
**A:** Security is implemented through:
- **JWT Validation**: At gateway and service level
- **Scope-Based Authorization**: Fine-grained permissions
- **Rate Limiting**: Per-user and per-IP limits
- **Input Validation**: Comprehensive request validation
- **Audit Logging**: Security event tracking

### 4. Schema Design

**Q: How do you design schemas for complex business objects?**
**A:** Schema design follows these principles:
- **Composition**: Reuse common schemas (Address, PaginationInfo)
- **Validation**: Comprehensive validation rules
- **Documentation**: Clear field descriptions and examples
- **Extensibility**: Design for future enhancements
- **Performance**: Optimize for serialization/deserialization

**Q: How do you handle optional vs. required fields?**
**A:** Field requirements are based on:
- **Business Logic**: Required fields for core functionality
- **User Experience**: Optional fields for enhanced features
- **Validation**: Server-side validation for all fields
- **Documentation**: Clear indication of field requirements

---

## 🔧 Technical Implementation Questions

### 1. Code Generation

**Q: How do you generate client SDKs from OpenAPI specs?**
**A:** Client SDK generation process:
- **OpenAPI Generator**: Generate TypeScript, Java, Python clients
- **Swagger Codegen**: Alternative code generation tool
- **Custom Templates**: Tailored to project needs
- **CI/CD Integration**: Automated generation on spec changes

**Q: How do you validate API implementations against contracts?**
**A:** Contract validation through:
- **Contract Testing**: Verify implementation matches spec
- **Schema Validation**: Runtime validation of requests/responses
- **Integration Testing**: End-to-end API testing
- **Monitoring**: Track API compliance metrics

### 2. Performance and Scalability

**Q: How do you optimize API performance?**
**A:** Performance optimization strategies:
- **Caching**: Response caching for read operations
- **Pagination**: Efficient data retrieval
- **Compression**: Response compression
- **CDN**: Content delivery for static resources
- **Load Balancing**: Distribute load across instances

**Q: How do you handle API rate limiting?**
**A:** Rate limiting implementation:
- **User-Based**: Per-user request limits
- **IP-Based**: Per-IP request limits
- **Endpoint-Specific**: Different limits for different endpoints
- **Graceful Degradation**: Return 429 status with retry headers

### 3. Security and Compliance

**Q: How do you ensure API security?**
**A:** Security measures include:
- **Authentication**: JWT token validation
- **Authorization**: Scope-based access control
- **Input Sanitization**: Prevent injection attacks
- **HTTPS**: Encrypt all communications
- **Audit Logging**: Track security events

**Q: How do you handle sensitive data in APIs?**
**A:** Sensitive data handling:
- **Data Masking**: Mask sensitive fields in responses
- **Encryption**: Encrypt data in transit and at rest
- **Access Control**: Limit access to sensitive data
- **Compliance**: Follow GDPR, PCI DSS requirements
- **Audit Trails**: Track data access and modifications

---

## 🎯 System Design Questions

### 1. API Gateway Integration

**Q: How does your API gateway integrate with OpenAPI specs?**
**A:** Gateway integration includes:
- **Route Configuration**: Automatic route discovery
- **Authentication**: JWT validation and propagation
- **Rate Limiting**: Per-service and global limits
- **Monitoring**: Request/response metrics
- **Error Handling**: Centralized error responses

**Q: How do you handle API versioning at the gateway level?**
**A:** Gateway versioning strategy:
- **Route Mapping**: Map versioned routes to services
- **Header Propagation**: Forward version headers
- **Backward Compatibility**: Support multiple versions
- **Migration**: Gradual migration to new versions

### 2. Service Communication

**Q: How do services communicate using the defined APIs?**
**A:** Service communication patterns:
- **Synchronous**: REST API calls for immediate responses
- **Asynchronous**: Event-driven communication via Kafka
- **Circuit Breakers**: Handle service failures gracefully
- **Retry Logic**: Automatic retry with exponential backoff
- **Timeout Handling**: Prevent cascading failures

**Q: How do you handle API changes in a microservices architecture?**
**A:** Change management process:
- **Backward Compatibility**: Maintain compatibility during transitions
- **Feature Flags**: Enable/disable new features
- **Gradual Rollout**: Deploy changes incrementally
- **Monitoring**: Track API usage and performance
- **Rollback Strategy**: Quick rollback if issues arise

---

## 📊 Metrics and Monitoring

### 1. API Performance Metrics

**Key Metrics to Track:**
- **Response Time**: Average, p95, p99 response times
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **Availability**: API uptime percentage
- **Usage Patterns**: Most/least used endpoints

### 2. API Quality Metrics

**Quality Indicators:**
- **Contract Compliance**: Implementation matches spec
- **Documentation Coverage**: All endpoints documented
- **Example Quality**: Realistic and comprehensive examples
- **Schema Validation**: All schemas are valid
- **Security Coverage**: All security requirements documented

---

## 🚀 Production Considerations

### 1. Deployment Strategy

**Deployment Process:**
- **Environment-Specific Specs**: Different specs for dev/staging/prod
- **Version Control**: Track spec changes in Git
- **Automated Validation**: Validate specs in CI/CD pipeline
- **Documentation Hosting**: Host interactive documentation
- **Client SDK Distribution**: Distribute generated SDKs

### 2. Monitoring and Alerting

**Monitoring Setup:**
- **API Metrics**: Track performance and usage
- **Error Tracking**: Monitor API errors and failures
- **Security Monitoring**: Track security events
- **Business Metrics**: Track business-relevant API usage
- **Alerting**: Proactive alerts for issues

---

## 📝 Best Practices Summary

### 1. Design Principles
- **RESTful Design**: Follow REST principles consistently
- **Resource-Oriented**: Design around business resources
- **Stateless**: APIs should be stateless
- **Cacheable**: Design for caching where appropriate
- **Layered System**: Support layered architecture

### 2. Documentation Standards
- **Comprehensive Examples**: Provide realistic examples
- **Clear Descriptions**: Explain business context
- **Error Scenarios**: Document all error cases
- **Authentication**: Clear authentication requirements
- **Rate Limits**: Document rate limiting policies

### 3. Security Guidelines
- **Authentication**: Always require authentication for sensitive operations
- **Authorization**: Implement fine-grained permissions
- **Input Validation**: Validate all inputs
- **Output Sanitization**: Sanitize sensitive data in responses
- **Audit Logging**: Log all security-relevant events

---

## 🎯 Interview Success Tips

### 1. Preparation
- **Review Your Implementation**: Understand every aspect of your OpenAPI specs
- **Practice Examples**: Be ready to walk through specific API examples
- **Understand Trade-offs**: Know the pros/cons of your design decisions
- **Prepare Metrics**: Have performance and quality metrics ready

### 2. Communication
- **Start High-Level**: Begin with architecture overview
- **Provide Examples**: Use concrete examples to illustrate points
- **Explain Rationale**: Justify your design decisions
- **Acknowledge Limitations**: Be honest about trade-offs and limitations

### 3. Problem-Solving
- **Think Aloud**: Explain your thought process
- **Consider Alternatives**: Discuss different approaches
- **Ask Clarifying Questions**: Ensure you understand the requirements
- **Propose Solutions**: Offer concrete solutions to problems

---

## 📚 Additional Resources

### Documentation
- [OpenAPI Specification](https://swagger.io/specification/)
- [REST API Design Guidelines](https://restfulapi.net/)
- [API Security Best Practices](https://owasp.org/www-project-api-security/)

### Tools
- [Swagger Editor](https://editor.swagger.io/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Postman](https://www.postman.com/) for API testing

### Standards
- [RFC 7807](https://tools.ietf.org/html/rfc7807) - Problem Details for HTTP APIs
- [RFC 6585](https://tools.ietf.org/html/rfc6585) - Additional HTTP Status Codes
- [JSON Schema](https://json-schema.org/) - JSON Schema specification

---

**Remember:** The key to success is demonstrating deep understanding of API design principles, practical implementation experience, and the ability to make informed trade-offs based on business requirements and technical constraints.
