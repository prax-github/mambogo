# 🎓 SEC-03 Per-Service JWT Validation - Interview Deep Dive

**Topic**: Distributed Security with Per-Service JWT Validation  
**Implementation**: SEC-03 Per-Service JWT Validation  
**Complexity Level**: Senior Software Engineer / Principal Engineer  
**Interview Categories**: Distributed Systems, Microservices Security, JWT, System Design

---

## 🎯 **The Big Picture - What Problem Are We Solving?**

**Interviewer Question**: *"How do you implement consistent security across multiple microservices while maintaining service autonomy?"*

**Your Answer Framework**:
- **Defense in Depth**: Multiple layers of security validation
- **Service Autonomy**: Each service validates its own security requirements
- **Consistent Implementation**: Standardized security patterns across services
- **Zero Trust**: Never trust, always verify - even internal requests

**Real-world Impact**: Without per-service validation, a compromised gateway or network could bypass all security, and services would be vulnerable to internal threats.

---

## 🏗️ **Architecture Decisions - The "Why" Behind Choices**

### 1. **Gateway + Service Validation vs Gateway-Only**

```
Defense in Depth (Our Choice):
┌─────────────┐    JWT Validation    ┌─────────────┐    JWT Validation    ┌─────────────┐
│   Client    │ ──────────────────→ │   Gateway   │ ──────────────────→ │ Microservice│
└─────────────┘                     └─────────────┘                     └─────────────┘
                                           │                                     │
                                           ▼                                     ▼
                                    ┌─────────────┐                     ┌─────────────┐
                                    │  Keycloak   │                     │  Keycloak   │
                                    │ (Primary)   │                     │ (Verify)    │
                                    └─────────────┘                     └─────────────┘

Gateway-Only Alternative:
┌─────────────┐    JWT Validation    ┌─────────────┐    User Headers     ┌─────────────┐
│   Client    │ ──────────────────→ │   Gateway   │ ──────────────────→ │ Microservice│
└─────────────┘                     └─────────────┘                     └─────────────┘
                                           │                                     │
                                           ▼                                     ▼
                                    ┌─────────────┐                     ┌─────────────┐
                                    │  Keycloak   │                     │   Trusts     │
                                    │ (Validate)  │                     │  Headers    │
                                    └─────────────┘                     └─────────────┘
```

**Trade-offs Analysis**:

| Aspect | Defense in Depth ✅ | Gateway-Only |
|--------|---------------------|--------------|
| **Security** | ✅ Multiple validation layers | ❌ Single point of failure |
| **Network Trust** | ✅ Zero trust internal network | ❌ Assumes secure internal network |
| **Service Autonomy** | ✅ Independent security policies | ❌ Dependent on gateway |
| **Performance** | ❌ Additional validation overhead | ✅ Single validation |
| **Complexity** | ❌ More configuration | ✅ Simpler setup |
| **Compliance** | ✅ Meets enterprise security standards | ❌ May not meet compliance |

**Interview Gold**: "We chose defense in depth because **security is non-negotiable** and **internal networks can be compromised**. The performance overhead is minimal compared to the security benefits."

### 2. **JWT Token Extraction Strategy**

**Challenge**: How do services efficiently extract user information from JWT tokens?

```java
// Centralized JWT Token Extractor
@Component
public class JwtTokenExtractor {
    
    // Thread-safe access to current JWT
    public Optional<Jwt> getJwt() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(JwtAuthenticationToken.class::cast)
            .map(JwtAuthenticationToken::getToken);
    }
    
    // Extract user ID from subject claim
    public Optional<String> getUserId() {
        return getJwt().map(Jwt::getSubject);
    }
    
    // Extract username from preferred_username claim
    public Optional<String> getUsername() {
        return getJwt().map(jwt -> jwt.getClaimAsString("preferred_username"));
    }
    
    // Extract roles with type safety
    public List<String> getUserRoles() {
        return getJwt()
            .map(jwt -> {
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null) {
                    Object rolesObj = realmAccess.get("roles");
                    if (rolesObj instanceof List) {
                        List<?> rolesList = (List<?>) rolesObj;
                        return rolesList.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    }
                }
                return new ArrayList<String>();
            })
            .orElse(new ArrayList<>());
    }
    
    // Convenience methods for role checking
    public boolean hasRole(String role) {
        return getUserRoles().contains(role);
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isUser() {
        return hasRole("USER");
    }
}
```

**Benefits of Centralized Extraction**:
- ✅ **Type Safety**: Handles JWT claim type conversions safely
- ✅ **Null Safety**: Graceful handling of missing claims
- ✅ **Reusability**: Consistent extraction across all services
- ✅ **Testability**: Easy to mock and unit test

---

## 🔧 **Technical Implementation Deep Dive**

### 1. **Service-Specific Security Configuration**

**Interviewer**: *"How do you configure different security requirements for different microservices?"*

**Product Service (Public + Secured Endpoints)**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ProductSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            
            // Service-specific authorization rules
            .authorizeHttpRequests(authz -> authz
                // Health and monitoring
                .requestMatchers("/actuator/**").permitAll()
                
                // Public catalog endpoints
                .requestMatchers("/api/catalog/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                
                // Admin endpoints require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server configuration
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Custom error handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            
            .build();
    }
}
```

**Cart Service (User-Only Endpoints)**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class CartSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            
            // Cart service - all endpoints require USER role
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/cart/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            
            .build();
    }
}
```

**Service-Specific Design Rationale**:
- **Product Service**: Mixed public/secured endpoints for catalog browsing
- **Cart Service**: User-only endpoints for personal cart management
- **Order Service**: User-only endpoints for order management
- **Payment Service**: User-only endpoints for payment processing

### 2. **Method-Level Security with @PreAuthorize**

```java
@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    private final JwtTokenExtractor jwtExtractor;
    private final CartService cartService;
    
    // Basic role-based authorization
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CartResponse> getUserCart() {
        String userId = jwtExtractor.getUserId()
            .orElseThrow(() -> new SecurityException("User ID not found"));
        
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
    
    // User can only access their own cart
    @GetMapping("/{cartId}")
    @PreAuthorize("hasRole('USER') and @cartService.isCartOwner(#cartId, authentication.name)")
    public ResponseEntity<CartResponse> getCart(@PathVariable String cartId) {
        Cart cart = cartService.getCart(cartId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
    
    // Admin can access any cart
    @GetMapping("/admin/{cartId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CartResponse> getCartAsAdmin(@PathVariable String cartId) {
        Cart cart = cartService.getCart(cartId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }
}
```

**Method Security Benefits**:
- ✅ **Fine-grained Control**: Method-level authorization rules
- ✅ **Business Logic Integration**: Custom security expressions
- ✅ **Declarative Security**: Clear, readable security annotations
- ✅ **Testable**: Easy to unit test security rules

### 3. **Custom Error Handling**

**Consistent Error Response Format**:
```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    private final ErrorProperties errorProperties;
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(errorProperties.getAuthentication().getCode())
            .message(errorProperties.getAuthentication().getMessage())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .service(errorProperties.getServiceName())
            .build();
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

**Externalized Error Configuration**:
```yaml
# application.yml
app:
  error:
    service-name: "cart-service"
    authentication:
      code: "AUTHENTICATION_FAILED"
      message: "Authentication required"
    authorization:
      code: "ACCESS_DENIED"
      message: "Insufficient permissions"
```

**Benefits of Externalized Configuration**:
- ✅ **Consistency**: Same error format across all services
- ✅ **Maintainability**: Easy to update error messages
- ✅ **Localization**: Support for multiple languages
- ✅ **Testing**: Easy to verify error responses

---

## 🚀 **Production Considerations - Advanced Topics**

### 1. **Performance Optimization**

**JWT Validation Caching**:
```java
@Configuration
public class JwtCacheConfig {
    
    @Bean
    public JwtDecoder cachedJwtDecoder() {
        String jwkSetUri = jwtProperties.getJwkSetUri();
        
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri(jwkSetUri)
            .cache(Duration.ofMinutes(5))  // Cache JWK for 5 minutes
            .build();
        
        // Add custom validators
        jwtDecoder.setJwtValidator(jwtValidator());
        
        return jwtDecoder;
    }
    
    @Bean
    public Converter<Jwt, JwtValidationResult> jwtValidator() {
        List<Converter<Jwt, JwtValidationResult>> validators = Arrays.asList(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(jwtProperties.getIssuer()),
            new JwtAudienceValidator(jwtProperties.getAudience())
        );
        
        return new DelegatingConverter<>(validators);
    }
}
```

**Connection Pool Optimization**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KC_ISSUER:http://localhost:8081/realms/ecommerce}
          # Optimize HTTP client for JWT validation
          connection-timeout: 3000
          read-timeout: 5000
          max-connections-per-route: 20
          max-connections-total: 100
```

### 2. **Resilience and Fault Tolerance**

**Circuit Breaker for JWT Validation**:
```java
@Component
public class ResilientJwtService {
    
    private final CircuitBreaker jwtValidationCircuitBreaker;
    private final JwtDecoder jwtDecoder;
    
    public ResilientJwtService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
        this.jwtValidationCircuitBreaker = CircuitBreaker.ofDefaults("jwt-validation");
        
        // Configure circuit breaker
        jwtValidationCircuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("JWT validation circuit breaker state: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));
    }
    
    public Jwt validateJwt(String token) {
        return jwtValidationCircuitBreaker.executeCallable(() -> 
            jwtDecoder.decode(token)
        );
    }
    
    @Recover
    public Jwt handleJwtValidationFailure(CircuitBreakerOpenException ex, String token) {
        // Fallback: Use cached validation or fail securely
        log.warn("JWT validation circuit breaker open, using fallback");
        
        // Option 1: Use cached public key for offline validation
        if (canValidateOffline(token)) {
            return validateWithCachedKey(token);
        }
        
        // Option 2: Fail securely
        throw new JwtValidationException("JWT validation service unavailable");
    }
}
```

**Graceful Degradation Strategy**:
```java
@Component
public class SecurityFallbackService {
    
    public boolean shouldAllowRequest(String endpoint, Exception validationError) {
        // Emergency mode configuration
        if (isEmergencyModeEnabled()) {
            log.warn("Emergency mode enabled: Allowing request to {} despite validation error", 
                endpoint);
            
            // Log security event for audit
            securityAuditService.logEmergencyAccess(endpoint, validationError);
            
            return isPublicEndpoint(endpoint);
        }
        
        return false;
    }
    
    private boolean isEmergencyModeEnabled() {
        // Check configuration or feature flag
        return configurationService.getBoolean("security.emergency-mode.enabled", false);
    }
}
```

### 3. **Security Monitoring and Auditing**

**Security Event Logging**:
```java
@Component
public class SecurityAuditService {
    
    private final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) event.getAuthentication();
        String userId = token.getToken().getSubject();
        String service = getCurrentServiceName();
        
        SecurityEvent securityEvent = SecurityEvent.builder()
            .type("AUTH_SUCCESS")
            .userId(userId)
            .service(service)
            .timestamp(Instant.now())
            .clientIp(getClientIp())
            .userAgent(getUserAgent())
            .build();
        
        securityLogger.info("Security event: {}", securityEvent);
        
        // Send to security monitoring system
        securityMonitoringService.recordEvent(securityEvent);
    }
    
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        SecurityEvent securityEvent = SecurityEvent.builder()
            .type("AUTH_FAILURE")
            .service(getCurrentServiceName())
            .timestamp(Instant.now())
            .clientIp(getClientIp())
            .error(event.getException().getMessage())
            .build();
        
        securityLogger.warn("Security event: {}", securityEvent);
        
        // Check for potential attacks
        if (isPotentialAttack(securityEvent)) {
            alertingService.sendSecurityAlert(securityEvent);
        }
    }
}
```

**Metrics Collection**:
```java
@Component
public class SecurityMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordAuthenticationAttempt(String service, String result) {
        Counter.builder("security.authentication.attempts")
            .tag("service", service)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordJwtValidationTime(String service, Duration duration) {
        Timer.builder("security.jwt.validation.duration")
            .tag("service", service)
            .register(meterRegistry)
            .record(duration);
    }
    
    public void recordSecurityViolation(String service, String violationType) {
        Counter.builder("security.violations")
            .tag("service", service)
            .tag("type", violationType)
            .register(meterRegistry)
            .increment();
    }
}
```

---

## 🎯 **Interview Questions You Can Now Answer**

### 1. **System Design Question**
*"Design security for a microservices architecture with 50+ services, ensuring consistent authentication while maintaining service autonomy"*

**Structured Answer Framework**:

1. **Requirements Analysis**:
   - Services: 50+ microservices with different security needs
   - Consistency: Uniform authentication and authorization patterns
   - Autonomy: Services can define their own authorization rules
   - Performance: Minimal security overhead (<5ms per request)

2. **High-Level Architecture**:
   ```
   [Client] → [API Gateway] → [Service Mesh] → [Microservices]
                │                  │               │
                ▼                  ▼               ▼
           [JWT Validation]   [mTLS/Service]  [Per-Service]
                              [Authentication] [Authorization]
   ```

3. **Security Layers**:
   - **Gateway Layer**: Initial JWT validation, rate limiting
   - **Service Mesh**: mTLS for service-to-service communication
   - **Service Layer**: Business logic authorization, method-level security
   - **Data Layer**: Row-level security, encryption at rest

4. **Standardization Strategy**:
   - **Security Libraries**: Shared JWT validation utilities
   - **Configuration Templates**: Standard security configurations
   - **Testing Framework**: Automated security testing
   - **Documentation**: Security implementation guidelines

5. **Monitoring & Compliance**:
   - **Centralized Logging**: All security events aggregated
   - **Metrics Dashboard**: Authentication success/failure rates
   - **Audit Trail**: Complete security event history
   - **Compliance Reporting**: Automated compliance checks

### 2. **Deep Technical Question**
*"How do you handle JWT token validation failures in a distributed system while maintaining service availability?"*

**Comprehensive Answer**:

```java
// Multi-layered Failure Handling Strategy
@Component
public class JwtValidationFailureHandler {
    
    private final CircuitBreaker circuitBreaker;
    private final JwtCache jwtCache;
    private final SecurityFallbackService fallbackService;
    
    public ValidationResult handleJwtValidation(String token, String endpoint) {
        try {
            // Primary validation path
            return circuitBreaker.executeCallable(() -> 
                validateJwtWithKeycloak(token));
                
        } catch (CircuitBreakerOpenException ex) {
            // Circuit breaker open - try cached validation
            return handleCircuitBreakerOpen(token, endpoint);
            
        } catch (JwtValidationException ex) {
            // JWT validation failed - check if recoverable
            return handleValidationFailure(token, endpoint, ex);
            
        } catch (Exception ex) {
            // Unexpected error - fail securely
            return handleUnexpectedError(token, endpoint, ex);
        }
    }
    
    private ValidationResult handleCircuitBreakerOpen(String token, String endpoint) {
        // Try offline validation with cached public keys
        if (jwtCache.hasValidPublicKey(extractKeyId(token))) {
            try {
                return validateJwtOffline(token);
            } catch (Exception ex) {
                log.warn("Offline JWT validation failed: {}", ex.getMessage());
            }
        }
        
        // Check if endpoint allows fallback
        if (fallbackService.shouldAllowRequest(endpoint, 
                new CircuitBreakerOpenException("JWT validation unavailable"))) {
            return ValidationResult.allowWithWarning("Emergency mode active");
        }
        
        return ValidationResult.deny("JWT validation service unavailable");
    }
    
    private ValidationResult handleValidationFailure(String token, String endpoint, 
                                                   JwtValidationException ex) {
        // Log security event
        securityAuditService.logValidationFailure(token, endpoint, ex);
        
        // Check for specific failure types
        if (ex.getCause() instanceof JwtExpiredException) {
            return ValidationResult.deny("Token expired", "TOKEN_EXPIRED");
        }
        
        if (ex.getCause() instanceof JwtSignatureException) {
            // Potential security threat
            alertingService.sendSecurityAlert("Invalid JWT signature detected", 
                Map.of("endpoint", endpoint, "error", ex.getMessage()));
            return ValidationResult.deny("Invalid token signature", "INVALID_SIGNATURE");
        }
        
        return ValidationResult.deny("Token validation failed", "VALIDATION_FAILED");
    }
}
```

**Failure Handling Strategies**:
- **Circuit Breaker**: Prevent cascade failures
- **Cached Validation**: Offline validation with cached public keys
- **Graceful Degradation**: Allow public endpoints during outages
- **Security Alerting**: Immediate notification of security threats
- **Audit Logging**: Complete failure event history

### 3. **Security Architecture Question**
*"What are the security trade-offs between gateway-only validation vs per-service validation, and how do you mitigate the risks?"*

**Detailed Analysis**:

| Security Aspect | Gateway-Only | Per-Service | Mitigation Strategy |
|-----------------|--------------|-------------|-------------------|
| **Network Trust** | Assumes secure internal network | Zero trust approach | **mTLS + Service mesh** |
| **Single Point of Failure** | Gateway compromise = full breach | Distributed validation | **Gateway clustering + monitoring** |
| **Performance** | Single validation overhead | Multiple validation overhead | **JWT caching + connection pooling** |
| **Consistency** | Centralized policy enforcement | Potential policy drift | **Shared security libraries** |
| **Service Coupling** | High coupling to gateway | Low coupling, autonomous | **Standardized interfaces** |
| **Compliance** | May not meet enterprise standards | Meets defense-in-depth requirements | **Audit trails + monitoring** |

**Risk Mitigation Implementation**:
```java
// Comprehensive Security Strategy
@Configuration
public class DefenseInDepthSecurityConfig {
    
    // Layer 1: Network Security (Service Mesh)
    @Bean
    public ServiceMeshSecurityConfig serviceMeshSecurity() {
        return ServiceMeshSecurityConfig.builder()
            .enableMutualTLS(true)
            .requireServiceAuthentication(true)
            .enforceNetworkPolicies(true)
            .build();
    }
    
    // Layer 2: Application Security (JWT Validation)
    @Bean
    public JwtSecurityConfig jwtSecurity() {
        return JwtSecurityConfig.builder()
            .enablePerServiceValidation(true)
            .cacheValidationResults(true)
            .enableCircuitBreaker(true)
            .fallbackToGatewayHeaders(false)  // Never trust headers alone
            .build();
    }
    
    // Layer 3: Business Logic Security (Method-level)
    @Bean
    public MethodSecurityConfig methodSecurity() {
        return MethodSecurityConfig.builder()
            .enablePreAuthorize(true)
            .enablePostAuthorize(true)
            .enableCustomExpressions(true)
            .auditSecurityDecisions(true)
            .build();
    }
    
    // Layer 4: Data Security (Row-level)
    @Bean
    public DataSecurityConfig dataSecurity() {
        return DataSecurityConfig.builder()
            .enableRowLevelSecurity(true)
            .encryptSensitiveFields(true)
            .auditDataAccess(true)
            .build();
    }
}
```

---

## 🔥 **Advanced Topics for Senior Interviews**

### 1. **Dynamic Security Policy Management**

```java
// Runtime security policy updates
@Component
public class DynamicSecurityPolicyManager {
    
    private final SecurityPolicyCache policyCache;
    private final SecurityPolicyRepository policyRepository;
    
    @EventListener
    public void handlePolicyUpdate(SecurityPolicyUpdateEvent event) {
        SecurityPolicy newPolicy = event.getPolicy();
        
        // Validate policy before applying
        if (validatePolicy(newPolicy)) {
            policyCache.updatePolicy(newPolicy.getServiceName(), newPolicy);
            
            // Notify all service instances
            messagingService.broadcast(new PolicyUpdateMessage(newPolicy));
            
            log.info("Security policy updated for service: {}", newPolicy.getServiceName());
        } else {
            log.error("Invalid security policy rejected: {}", newPolicy);
        }
    }
    
    public boolean isAuthorized(String service, String endpoint, String userId, List<String> roles) {
        SecurityPolicy policy = policyCache.getPolicy(service);
        
        return policy.getEndpointRules().stream()
            .filter(rule -> rule.matches(endpoint))
            .anyMatch(rule -> rule.isAuthorized(userId, roles));
    }
}
```

### 2. **Advanced JWT Claims Processing**

```java
// Custom JWT claims processor
@Component
public class AdvancedJwtClaimsProcessor {
    
    public UserContext processJwtClaims(Jwt jwt) {
        UserContextBuilder builder = UserContext.builder();
        
        // Standard claims
        builder.userId(jwt.getSubject())
               .username(jwt.getClaimAsString("preferred_username"))
               .email(jwt.getClaimAsString("email"));
        
        // Custom claims processing
        processCustomClaims(jwt, builder);
        
        // Role hierarchy processing
        List<String> roles = extractRoles(jwt);
        List<String> expandedRoles = expandRoleHierarchy(roles);
        builder.roles(expandedRoles);
        
        // Tenant information
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId != null) {
            TenantContext tenantContext = tenantService.getTenantContext(tenantId);
            builder.tenantContext(tenantContext);
        }
        
        // Feature flags
        List<String> features = jwt.getClaimAsList("features");
        builder.enabledFeatures(features != null ? features : Collections.emptyList());
        
        return builder.build();
    }
    
    private void processCustomClaims(Jwt jwt, UserContextBuilder builder) {
        // Organization hierarchy
        String orgId = jwt.getClaimAsString("org_id");
        if (orgId != null) {
            OrganizationContext orgContext = organizationService.getContext(orgId);
            builder.organizationContext(orgContext);
        }
        
        // Permission sets
        List<String> permissions = jwt.getClaimAsList("permissions");
        if (permissions != null) {
            Set<Permission> permissionSet = permissions.stream()
                .map(permissionService::getPermission)
                .collect(Collectors.toSet());
            builder.permissions(permissionSet);
        }
        
        // Geographic restrictions
        String allowedRegions = jwt.getClaimAsString("allowed_regions");
        if (allowedRegions != null) {
            builder.allowedRegions(Arrays.asList(allowedRegions.split(",")));
        }
    }
}
```

### 3. **Security Testing Framework**

```java
// Automated security testing
@Component
public class SecurityTestFramework {
    
    public SecurityTestResult runSecurityTests(String serviceName) {
        SecurityTestSuite testSuite = SecurityTestSuite.builder()
            .service(serviceName)
            .addTest(new JwtValidationTest())
            .addTest(new AuthorizationTest())
            .addTest(new InputValidationTest())
            .addTest(new ErrorHandlingTest())
            .build();
        
        return testSuite.execute();
    }
    
    @Test
    public void testJwtValidation() {
        // Test valid JWT
        String validJwt = generateValidJwt();
        assertThat(jwtValidator.validate(validJwt)).isValid();
        
        // Test expired JWT
        String expiredJwt = generateExpiredJwt();
        assertThat(jwtValidator.validate(expiredJwt)).isInvalid()
            .hasErrorCode("TOKEN_EXPIRED");
        
        // Test invalid signature
        String invalidJwt = generateInvalidSignatureJwt();
        assertThat(jwtValidator.validate(invalidJwt)).isInvalid()
            .hasErrorCode("INVALID_SIGNATURE");
        
        // Test missing claims
        String incompleteJwt = generateIncompleteJwt();
        assertThat(jwtValidator.validate(incompleteJwt)).isInvalid()
            .hasErrorCode("MISSING_CLAIMS");
    }
    
    @Test
    public void testAuthorizationMatrix() {
        // Test role-based access
        SecurityTestMatrix matrix = SecurityTestMatrix.builder()
            .endpoint("/api/cart")
            .method("GET")
            .addRole("USER", AccessResult.ALLOWED)
            .addRole("ADMIN", AccessResult.ALLOWED)
            .addRole("GUEST", AccessResult.DENIED)
            .build();
        
        matrix.execute().assertAllPassed();
    }
}
```

---

## 🎖️ **What This Demonstrates to Interviewers**

### Technical Skills ✅
- **Distributed Security**: Understanding of security in microservices
- **JWT Expertise**: Deep knowledge of token validation and claims processing
- **Spring Security**: Advanced configuration and method-level security
- **Resilience Patterns**: Circuit breakers, fallback strategies, graceful degradation

### Engineering Maturity ✅
- **Defense in Depth**: Multiple layers of security validation
- **Zero Trust**: Never trust, always verify approach
- **Production Readiness**: Monitoring, alerting, performance optimization
- **Testing**: Comprehensive security testing framework

### Real-world Experience ✅
- **Service Autonomy**: Balancing consistency with independence
- **Performance**: Optimizing security without sacrificing speed
- **Compliance**: Meeting enterprise security standards
- **Operational Excellence**: Monitoring, auditing, incident response

---

## 🚀 **Key Takeaways for Interviews**

### 1. **Defense in Depth is Critical**
Multiple layers of security validation provide better protection than single points.

### 2. **Service Autonomy vs Consistency**
Balance standardized security patterns with service-specific requirements.

### 3. **Performance Matters**
Security shouldn't significantly impact application performance - optimize with caching and connection pooling.

### 4. **Failure Handling is Key**
Always have fallback strategies for when security services are unavailable.

### 5. **Monitoring and Auditing**
Comprehensive security event logging and monitoring are essential for compliance and incident response.

---

## 📚 **Further Learning & Related Topics**

### Next Level Topics:
- **Service Mesh Security**: Istio, Linkerd security features
- **Policy as Code**: Open Policy Agent (OPA) integration
- **Secrets Management**: Vault, Kubernetes secrets
- **Security Scanning**: Static analysis, dependency scanning

### Related System Design Questions:
- Design security for a multi-tenant SaaS platform
- Design authentication for a global microservices platform
- Design security monitoring for 1000+ microservices
- Design zero-trust security architecture

---

**Remember**: This implementation showcases **enterprise-grade distributed security** - exactly what senior engineering interviews are looking for! 🎯
