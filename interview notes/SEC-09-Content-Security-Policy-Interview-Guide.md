# SEC-09: Content Security Policy (CSP) Headers - Interview Guide

## Overview
This guide covers the implementation of advanced Content Security Policy (CSP) headers in a Spring Cloud Gateway, including dynamic policy generation, violation reporting, and comprehensive monitoring.

## Technical Implementation Deep Dive

### 1. Content Security Policy Fundamentals

**Q: What is Content Security Policy and how does it prevent XSS attacks?**

**A:** Content Security Policy (CSP) is a browser security standard that helps prevent Cross-Site Scripting (XSS) and data injection attacks by:

- **Allowlist Mechanism**: Defining trusted sources for scripts, styles, images, and other resources
- **Execution Prevention**: Blocking inline scripts/styles unless explicitly allowed via nonces or hashes
- **Resource Restriction**: Controlling where the browser can load resources from
- **Violation Reporting**: Sending reports when policy violations occur

**Implementation Details:**
```yaml
csp:
  enabled: true
  directives:
    script-src: ["'self'", "'nonce-{nonce}'"]
    style-src: ["'self'", "'unsafe-inline'"]
    img-src: ["'self'", "data:", "https:"]
    connect-src: ["'self'", "https://api.example.com"]
```

### 2. Dynamic Policy Generation

**Q: How do you implement dynamic CSP policy generation in a microservices environment?**

**A:** Our implementation uses a multi-layered approach:

**Architecture Components:**
1. **CspPolicyProperties**: Configuration-driven policy definitions
2. **CspPolicyManager**: Dynamic policy generation based on request context
3. **CspHeadersFilter**: Gateway filter for header application
4. **Environment-specific configs**: Different policies for dev/prod environments

**Key Features:**
```java
public String generateCspHeader(String path, String nonce) {
    StringBuilder policy = new StringBuilder();
    
    for (Map.Entry<String, List<String>> directive : directives.entrySet()) {
        policy.append(directive.getKey()).append(" ");
        
        List<String> sources = directive.getValue().stream()
            .map(source -> replaceNonce(source, nonce))
            .collect(Collectors.toList());
            
        policy.append(String.join(" ", sources)).append("; ");
    }
    
    return policy.toString().trim();
}
```

### 3. Nonce and Hash Implementation

**Q: Explain the difference between CSP nonces and hashes, and when to use each.**

**A:** 

**Nonces (Number Used Once):**
- **Purpose**: Allow specific inline scripts/styles by providing a unique token
- **Generation**: Cryptographically secure random string per request
- **Use Case**: Dynamic content that changes per request
- **Implementation**: `'nonce-abc123def456'`

**Hashes:**
- **Purpose**: Allow specific static inline scripts/styles by content hash
- **Generation**: SHA-256/384/512 hash of the exact content
- **Use Case**: Static inline content that doesn't change
- **Implementation**: `'sha256-xyz789abc123...'`

**Our Implementation:**
```java
public String generateNonce() {
    byte[] nonceBytes = new byte[16];
    secureRandom.nextBytes(nonceBytes);
    return Base64.getEncoder().encodeToString(nonceBytes);
}

public String calculateHash(String content, String algorithm) {
    MessageDigest digest = MessageDigest.getInstance(algorithm);
    byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hashBytes);
}
```

### 4. Violation Reporting System

**Q: How do you implement and process CSP violation reports?**

**A:** Our violation reporting system includes:

**Browser Integration:**
```http
Content-Security-Policy: default-src 'self'; report-uri /api/csp/violations
```

**Violation Controller:**
```java
@PostMapping("/api/csp/violations")
public ResponseEntity<Void> reportViolation(@RequestBody CspViolationEvent violation) {
    logger.warn("CSP violation reported: {} blocked {} on {}",
        violation.getViolatedDirective(),
        violation.getBlockedUri(),
        violation.getDocumentUri());
    
    cspMetricsCollector.recordCspViolation(
        violation.getViolatedDirective(),
        violation.getSourceFile(),
        violation.getDocumentUri()
    );
    
    cspViolationProcessor.processViolation(violation);
    return ResponseEntity.ok().build();
}
```

**Violation Processing:**
- **Real-time Analysis**: Immediate security threat assessment
- **Pattern Detection**: Identifying systematic attack attempts
- **Metrics Collection**: Prometheus metrics for monitoring
- **Alert Generation**: Critical violation notifications

### 5. Environment-Specific Policies

**Q: How do you handle different CSP requirements across development, staging, and production environments?**

**A:** We implement environment-specific CSP configurations:

**Development Environment:**
```yaml
# application-local.yml
csp:
  directives:
    script-src: ["'self'", "'unsafe-inline'", "'unsafe-eval'", "http://localhost:*"]
    connect-src: ["'self'", "http://localhost:*", "ws://localhost:*"]
```

**Production Environment:**
```yaml
# application-prod.yml
csp:
  directives:
    script-src: ["'self'", "'nonce-{nonce}'"]
    connect-src: ["'self'", "https://api.mambogo.com"]
    upgrade-insecure-requests: true
```

**Configuration Strategy:**
1. **Relaxed Development**: Allow inline scripts for debugging
2. **Strict Production**: Enforce nonces, HTTPS-only
3. **Gradual Enforcement**: Report-only mode before enforcement
4. **Domain-specific**: Different policies for admin vs. public areas

### 6. Monitoring and Metrics

**Q: What metrics should you collect for CSP monitoring and how do you implement them?**

**A:** Comprehensive CSP metrics collection:

**Key Metrics:**
```java
// Policy Application Metrics
Counter.builder("csp_policies_applied_total")
    .tag("policy_type", policyType)
    .tag("environment", environment)
    .register(meterRegistry)
    .increment();

// Violation Metrics
Counter.builder("csp_violations_total")
    .tag("directive", violatedDirective)
    .tag("origin", documentOrigin)
    .register(meterRegistry)
    .increment();

// Performance Metrics
Timer.builder("csp_processing_duration_seconds")
    .tag("operation", "policy_generation")
    .register(meterRegistry)
    .record(duration);
```

**Monitoring Dashboard:**
- **Violation Trends**: Track violation patterns over time
- **Policy Effectiveness**: Measure blocked vs. allowed requests
- **Performance Impact**: CSP processing overhead
- **Coverage Analysis**: Which directives are most effective

### 7. Production Considerations

**Q: What are the key considerations when deploying CSP in a production environment?**

**A:** Critical production considerations:

**Performance Optimization:**
- **Policy Caching**: Cache generated policies to reduce overhead
- **Nonce Pooling**: Pre-generate nonces for high-traffic scenarios
- **Header Compression**: Optimize CSP header size
- **Conditional Application**: Apply CSP only to relevant content types

**Security Best Practices:**
- **Gradual Rollout**: Start with report-only mode
- **Violation Analysis**: Monitor violation patterns before enforcement
- **Fallback Policies**: Handle CSP-unsupported browsers gracefully
- **Emergency Bypass**: Ability to disable CSP during incidents

**Operational Excellence:**
- **Automated Testing**: CSP policy validation in CI/CD
- **Change Management**: Controlled policy updates
- **Documentation**: Clear policy rationale and troubleshooting guides
- **Team Training**: Ensure development teams understand CSP implications

## Common Interview Questions

### System Design Questions

**Q: Design a CSP implementation for a large-scale e-commerce platform with multiple frontends.**

**A:** Architecture considerations:
1. **Centralized Policy Management**: Single source of truth for CSP policies
2. **Service-Specific Policies**: Different policies for admin, customer, mobile
3. **CDN Integration**: CSP header propagation through CDN
4. **Real-time Updates**: Hot-reloading of policy changes
5. **Compliance Reporting**: Automated compliance verification

### Technical Deep Dive

**Q: How would you debug a CSP violation in production?**

**A:** Debugging process:
1. **Violation Report Analysis**: Parse violation details
2. **Source Identification**: Locate violating code/resource
3. **Policy Review**: Verify if violation is legitimate
4. **Impact Assessment**: Determine if attack or false positive
5. **Remediation**: Update policy or fix code as appropriate

### Trade-off Analysis

**Q: What are the trade-offs between strict CSP policies and developer experience?**

**A:** Key trade-offs:
- **Security vs. Flexibility**: Strict policies may break third-party integrations
- **Performance vs. Security**: Nonce generation adds processing overhead
- **Maintenance vs. Coverage**: Complex policies require more maintenance
- **User Experience vs. Security**: Overly strict policies may break functionality

## Advanced Topics

### CSP Level 3 Features
- **Trusted Types**: Prevention of DOM XSS
- **Unsafe Hashes**: Selective inline script allowlisting
- **Strict Dynamic**: Propagating trust through script execution

### Integration Patterns
- **API Gateway Integration**: Consistent CSP across microservices
- **CDN Configuration**: Edge-level CSP enforcement
- **Mobile App CSP**: Cordova/hybrid app security

### Compliance and Regulations
- **GDPR Considerations**: Data protection in violation reporting
- **PCI DSS**: CSP requirements for payment processing
- **SOC 2**: Security controls documentation

## Implementation Lessons Learned

### What Worked Well
1. **Modular Design**: Separate components for different CSP aspects
2. **Configuration-Driven**: Environment-specific policies via YAML
3. **Comprehensive Monitoring**: Detailed metrics and violation tracking
4. **Gradual Rollout**: Report-only mode before enforcement

### Challenges Encountered
1. **Third-party Compatibility**: Some vendors don't support CSP
2. **Legacy Code**: Inline scripts in legacy components
3. **Development Workflow**: CSP breaking local development setups
4. **Performance Tuning**: Optimizing nonce generation overhead

### Best Practices Established
1. **Policy Testing**: Automated CSP policy validation
2. **Documentation**: Clear guidelines for developers
3. **Exception Management**: Controlled process for policy exceptions
4. **Regular Reviews**: Periodic policy effectiveness assessment

## Sample Implementation Code

### Complete CSP Filter Implementation
```java
@Component
@Order(-200)
public class CspHeadersFilter implements GlobalFilter {
    
    private final CspPolicyManager policyManager;
    private final CspMetricsCollector metricsCollector;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String nonce = policyManager.generateNonce();
            String cspHeader = policyManager.generateCspHeader(
                exchange.getRequest().getPath().value(), nonce);
            
            exchange.getResponse().getHeaders().set("Content-Security-Policy", cspHeader);
            
            metricsCollector.recordCspPolicyApplied(
                exchange.getRequest().getPath().value(),
                getOrigin(exchange.getRequest()),
                "Content-Security-Policy"
            );
        }));
    }
}
```

This interview guide demonstrates deep technical knowledge of CSP implementation, real-world considerations, and the ability to design scalable security solutions in a microservices architecture.
