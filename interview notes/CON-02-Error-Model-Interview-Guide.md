# CON-02: Error Model Interview Guide

**Task ID:** CON-02  
**Task Name:** Error model (problem+json, codes)  
**Status:** ✅ Completed  
**Date:** 2025-08-29  
**Owner:** Prashant Sinha  

---

## 🎯 Interview Context

This guide covers the implementation of a standardized error response model following RFC 7807 (Problem Details for HTTP APIs) across all microservices. The interviewer will assess your understanding of error handling patterns, security considerations, and traceability in distributed systems.

---

## 📋 Key Topics to Master

### 1. RFC 7807 Problem Details Standard

#### Core Concepts
- **RFC 7807**: Standard for HTTP API problem details
- **Problem Details**: Structured error information
- **Traceability**: Correlation IDs for debugging
- **Security**: Safe error messages for production

#### Standard Error Response Structure
```json
{
  "type": "https://api.mambogo.com/errors/validation-error",
  "title": "Request validation failed",
  "status": 400,
  "detail": "The request contains invalid data",
  "instance": "/api/orders",
  "traceId": "abc123-def456",
  "timestamp": "2025-08-28T12:00:00Z",
  "code": "VALIDATION_ERROR",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Invalid email format",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

### 2. Error Code Standardization

#### Error Code Categories
```java
// Validation Errors
VALIDATION_ERROR = "VALIDATION_ERROR"
BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION"
CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION"
INVALID_INPUT_FORMAT = "INVALID_INPUT_FORMAT"
REQUIRED_FIELD_MISSING = "REQUIRED_FIELD_MISSING"

// Security Errors
AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
AUTHORIZATION_FAILED = "AUTHORIZATION_FAILED"
XSS_DETECTED = "XSS_DETECTED"
SQL_INJECTION_DETECTED = "SQL_INJECTION_DETECTED"
PATH_TRAVERSAL_DETECTED = "PATH_TRAVERSAL_DETECTED"

// Business Errors
RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND"
DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE"
SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"
INTERNAL_ERROR = "INTERNAL_ERROR"
```

#### Service-Specific Error Codes
```java
// Order Service
ORDER_AMOUNT_INVALID = "ORDER_AMOUNT_INVALID"
ORDER_ITEMS_LIMIT_EXCEEDED = "ORDER_ITEMS_LIMIT_EXCEEDED"
ORDER_TIMEOUT = "ORDER_TIMEOUT"
IDEMPOTENCY_KEY_MISSING = "IDEMPOTENCY_KEY_MISSING"

// Payment Service
PAYMENT_AMOUNT_INVALID = "PAYMENT_AMOUNT_INVALID"
PAYMENT_METHOD_INVALID = "PAYMENT_METHOD_INVALID"
INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"
PAYMENT_PROCESSOR_ERROR = "PAYMENT_PROCESSOR_ERROR"
```

### 3. Error Handling Architecture

#### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Request validation failed")
            .traceId(getTraceId(request))
            .timestamp(Instant.now().toString())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .service("order-service")
            .fieldErrors(extractFieldErrors(ex))
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .code("BUSINESS_RULE_VIOLATION")
            .message(ex.getMessage())
            .traceId(getTraceId(request))
            .timestamp(Instant.now().toString())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .service("order-service")
            .build();
        
        return ResponseEntity.unprocessableEntity().body(error);
    }
}
```

### 4. Security Integration

#### Security Audit Logging
```java
@Component
public class SecurityAuditLogger {
    
    public void logValidationFailure(String userId, String requestUri, 
                                   String errorType, String errorMessage, String clientIp) {
        securityLogger.warn("Validation failure - User: {}, URI: {}, Type: {}, Message: {}, IP: {}", 
                          userId, requestUri, errorType, errorMessage, clientIp);
    }
    
    public void logSecurityViolation(String userId, String requestUri, 
                                   String violationType, String details, String clientIp) {
        securityLogger.error("Security violation - User: {}, URI: {}, Type: {}, Details: {}, IP: {}", 
                           userId, requestUri, violationType, details, clientIp);
    }
}
```

#### Input Sanitization
```java
private Object sanitizeForLogging(Object value) {
    if (value == null) {
        return null;
    }
    
    String stringValue = value.toString();
    
    // Remove sensitive patterns
    stringValue = stringValue.replaceAll("(?i)(password|token|key|secret)=[^\\s&]*", "$1=***");
    
    // Truncate long values
    if (stringValue.length() > 100) {
        stringValue = stringValue.substring(0, 97) + "...";
    }
    
    return stringValue;
}
```

---

## 🧠 Deep Dive Questions

### 1. Error Handling Principles

**Q: Why did you choose RFC 7807 over other error response standards?**
**A:** RFC 7807 provides several advantages:
- **Standardization**: Industry-standard approach
- **Structured Data**: Consistent error format across services
- **Extensibility**: Easy to add custom fields
- **Security**: Built-in security considerations
- **Traceability**: Support for correlation IDs

**Q: How do you ensure error responses don't leak sensitive information?**
**A:** Security measures include:
- **Input Sanitization**: Remove sensitive data from error messages
- **Error Classification**: Different detail levels for different environments
- **Audit Logging**: Log security-relevant errors separately
- **Environment-Based Responses**: Different error details for dev vs. prod
- **Pattern Matching**: Detect and sanitize sensitive patterns

### 2. Traceability and Debugging

**Q: How do you implement traceability across microservices?**
**A:** Traceability implementation:
- **Correlation IDs**: Propagate trace IDs across service calls
- **MDC Integration**: Use MDC for structured logging
- **Header Propagation**: Forward trace headers in HTTP calls
- **Centralized Logging**: Aggregate logs for correlation
- **Distributed Tracing**: Integrate with Zipkin/Jaeger

**Q: How do you handle error correlation in asynchronous operations?**
**A:** Async error correlation:
- **Event Correlation**: Include trace IDs in Kafka events
- **Outbox Pattern**: Store correlation IDs with outbox events
- **Dead Letter Queues**: Include trace IDs in DLQ messages
- **Retry Mechanisms**: Preserve correlation IDs during retries
- **Monitoring**: Track error patterns across async flows

### 3. Error Code Management

**Q: How do you manage error codes across multiple services?**
**A:** Error code management strategy:
- **Centralized Constants**: Shared error code definitions
- **Service Prefixes**: Prefix codes with service names
- **Versioning**: Support multiple error code versions
- **Documentation**: Maintain error code documentation
- **Deprecation**: Clear deprecation strategy for old codes

**Q: How do you handle internationalization of error messages?**
**A:** I18n implementation:
- **Message Codes**: Use message codes instead of hardcoded strings
- **Locale Detection**: Detect user locale from headers
- **Resource Bundles**: Store messages in resource files
- **Fallback Strategy**: Default to English if locale not supported
- **Context-Aware Messages**: Include relevant context in messages

### 4. Performance and Monitoring

**Q: How do you ensure error handling doesn't impact performance?**
**A:** Performance optimization:
- **Async Logging**: Use async logging for non-critical errors
- **Error Caching**: Cache common error responses
- **Lazy Evaluation**: Only evaluate error details when needed
- **Resource Limits**: Limit error response size
- **Monitoring**: Track error handling performance

**Q: How do you monitor and alert on error patterns?**
**A:** Monitoring strategy:
- **Error Rate Tracking**: Monitor error rates by service and endpoint
- **Pattern Detection**: Identify recurring error patterns
- **Threshold Alerts**: Alert when error rates exceed thresholds
- **Business Impact**: Track errors by business impact
- **Trend Analysis**: Analyze error trends over time

---

## 🔧 Technical Implementation Questions

### 1. Exception Handling Architecture

**Q: How do you structure your exception hierarchy?**
**A:** Exception hierarchy design:
- **Base Exceptions**: Common base classes for different error types
- **Service Exceptions**: Service-specific exception classes
- **Business Exceptions**: Domain-specific business exceptions
- **Technical Exceptions**: Infrastructure and technical exceptions
- **Custom Exceptions**: Extend standard exceptions for specific needs

**Q: How do you handle exceptions in async operations?**
**A:** Async exception handling:
- **CompletableFuture**: Use CompletableFuture for async error handling
- **Reactive Streams**: Use Mono/Flux error operators
- **Circuit Breakers**: Handle service failures gracefully
- **Retry Logic**: Implement exponential backoff retry
- **Dead Letter Queues**: Route failed messages to DLQ

### 2. Security Considerations

**Q: How do you prevent information disclosure in error messages?**
**A:** Information disclosure prevention:
- **Environment-Based Responses**: Different detail levels per environment
- **Input Validation**: Validate and sanitize all inputs
- **Error Classification**: Classify errors by sensitivity
- **Audit Logging**: Log sensitive errors separately
- **Pattern Matching**: Detect and sanitize sensitive data

**Q: How do you handle security-related errors?**
**A:** Security error handling:
- **Security Logging**: Log security events separately
- **Rate Limiting**: Limit error responses for security events
- **Alerting**: Immediate alerts for security violations
- **Forensics**: Preserve evidence for security investigations
- **Compliance**: Ensure compliance with security standards

### 3. Integration and Testing

**Q: How do you test error handling comprehensively?**
**A:** Error testing strategy:
- **Unit Testing**: Test individual error handlers
- **Integration Testing**: Test error flows across services
- **Contract Testing**: Verify error responses match contracts
- **Chaos Testing**: Test error handling under failure conditions
- **Security Testing**: Test error handling for security scenarios

**Q: How do you ensure error responses are consistent across services?**
**A:** Consistency strategies:
- **Shared Libraries**: Common error handling libraries
- **Contract Validation**: Validate error responses against schemas
- **Integration Testing**: Test error responses across services
- **Monitoring**: Monitor error response consistency
- **Documentation**: Document error response standards

---

## 🎯 System Design Questions

### 1. Distributed Error Handling

**Q: How do you handle errors in a distributed system?**
**A:** Distributed error handling:
- **Circuit Breakers**: Prevent cascading failures
- **Retry Logic**: Implement intelligent retry strategies
- **Fallback Mechanisms**: Provide fallback responses
- **Timeout Handling**: Set appropriate timeouts
- **Error Propagation**: Propagate errors appropriately

**Q: How do you handle partial failures in microservices?**
**A:** Partial failure handling:
- **Saga Pattern**: Use sagas for distributed transactions
- **Compensation Logic**: Implement compensation for failures
- **Event Sourcing**: Use events for failure recovery
- **Outbox Pattern**: Ensure reliable message delivery
- **Monitoring**: Monitor partial failure patterns

### 2. Error Recovery and Resilience

**Q: How do you implement error recovery mechanisms?**
**A:** Error recovery implementation:
- **Automatic Retry**: Retry failed operations automatically
- **Manual Recovery**: Provide manual recovery procedures
- **Data Consistency**: Ensure data consistency after errors
- **State Management**: Track system state for recovery
- **Documentation**: Document recovery procedures

**Q: How do you handle errors in event-driven architectures?**
**A:** Event-driven error handling:
- **Dead Letter Queues**: Route failed events to DLQ
- **Event Replay**: Replay events after error resolution
- **Event Correlation**: Correlate events for error tracking
- **Compensation Events**: Send compensation events for failures
- **Monitoring**: Monitor event processing errors

---

## 📊 Metrics and Monitoring

### 1. Error Metrics

**Key Metrics to Track:**
- **Error Rate**: Percentage of requests that result in errors
- **Error Distribution**: Distribution of errors by type and service
- **Response Time**: Impact of error handling on response times
- **User Impact**: Errors that directly impact user experience
- **Business Impact**: Errors that affect business operations

### 2. Error Quality Metrics

**Quality Indicators:**
- **Error Clarity**: How clear and actionable error messages are
- **Error Completeness**: Whether errors include all necessary information
- **Error Consistency**: Consistency of error formats across services
- **Error Security**: Whether errors expose sensitive information
- **Error Traceability**: Whether errors can be traced and debugged

---

## 🚀 Production Considerations

### 1. Error Handling in Production

**Production Requirements:**
- **Performance**: Error handling must not impact performance
- **Security**: Errors must not expose sensitive information
- **Monitoring**: Comprehensive error monitoring and alerting
- **Recovery**: Automated and manual recovery procedures
- **Documentation**: Clear documentation of error scenarios

### 2. Error Handling Evolution

**Evolution Strategy:**
- **Backward Compatibility**: Maintain compatibility during changes
- **Versioning**: Version error response formats
- **Migration**: Gradual migration to new error formats
- **Testing**: Comprehensive testing of error handling changes
- **Documentation**: Update documentation for error handling changes

---

## 📝 Best Practices Summary

### 1. Error Design Principles
- **Consistency**: Use consistent error formats across services
- **Clarity**: Provide clear and actionable error messages
- **Security**: Never expose sensitive information in errors
- **Traceability**: Include correlation IDs for debugging
- **Performance**: Ensure error handling doesn't impact performance

### 2. Error Handling Standards
- **RFC 7807 Compliance**: Follow RFC 7807 for error responses
- **Structured Logging**: Use structured logging for errors
- **Audit Logging**: Log security-relevant errors separately
- **Monitoring**: Monitor error patterns and trends
- **Documentation**: Document error codes and scenarios

### 3. Security Guidelines
- **Input Sanitization**: Sanitize all inputs before processing
- **Error Classification**: Classify errors by sensitivity
- **Environment-Based Responses**: Different detail levels per environment
- **Audit Trails**: Maintain audit trails for security events
- **Compliance**: Ensure compliance with security standards

---

## 🎯 Interview Success Tips

### 1. Preparation
- **Review Your Implementation**: Understand every aspect of your error handling
- **Practice Examples**: Be ready to walk through specific error scenarios
- **Understand Trade-offs**: Know the pros/cons of your design decisions
- **Prepare Metrics**: Have error handling metrics ready

### 2. Communication
- **Start High-Level**: Begin with error handling architecture overview
- **Provide Examples**: Use concrete examples to illustrate points
- **Explain Rationale**: Justify your error handling decisions
- **Acknowledge Limitations**: Be honest about trade-offs and limitations

### 3. Problem-Solving
- **Think Aloud**: Explain your thought process for error scenarios
- **Consider Alternatives**: Discuss different error handling approaches
- **Ask Clarifying Questions**: Ensure you understand the error requirements
- **Propose Solutions**: Offer concrete solutions to error handling problems

---

## 📚 Additional Resources

### Documentation
- [RFC 7807](https://tools.ietf.org/html/rfc7807) - Problem Details for HTTP APIs
- [OWASP API Security](https://owasp.org/www-project-api-security/)
- [Spring Boot Error Handling](https://spring.io/guides/gs/actuator-service/)

### Tools
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/) for error monitoring
- [Zipkin](https://zipkin.io/) for distributed tracing
- [ELK Stack](https://www.elastic.co/what-is/elk-stack) for log aggregation

### Standards
- [RFC 7807](https://tools.ietf.org/html/rfc7807) - Problem Details for HTTP APIs
- [RFC 6585](https://tools.ietf.org/html/rfc6585) - Additional HTTP Status Codes
- [JSON Schema](https://json-schema.org/) - JSON Schema specification

---

**Remember:** The key to success is demonstrating deep understanding of error handling principles, security considerations, and practical implementation experience in distributed systems.
