# 🎓 SEC-04 Service Scopes - Interview Deep Dive

**Topic**: OAuth2 Service Scopes and Fine-Grained Authorization  
**Implementation**: SEC-04 Service Scopes (`product:read`, `cart:manage`, `order:write`)  
**Complexity Level**: Principal Engineer / Staff Engineer  
**Interview Categories**: OAuth2 Advanced, Authorization Architecture, Security Design

---

## 🎯 **The Big Picture - What Problem Are We Solving?**

**Interviewer Question**: *"How do you implement fine-grained authorization that goes beyond simple role-based access control in a microservices architecture?"*

**Your Answer Framework**:
- **Granular Permissions**: Move beyond coarse-grained roles to specific action-based permissions
- **Service Boundaries**: Each microservice owns its authorization domain
- **Principle of Least Privilege**: Users get only the minimum permissions needed
- **OAuth2 Standards**: Leverage industry-standard scope-based authorization

**Real-world Impact**: Without fine-grained scopes, a user with "USER" role might have access to all user operations across all services, violating the principle of least privilege and creating security risks.

---

## 🏗️ **Architecture Decisions - The "Why" Behind Choices**

### 1. **Role-Based vs Scope-Based Authorization**

```
Traditional Role-Based (Coarse-Grained):
┌─────────────┐    ROLE_USER     ┌─────────────┐    All User      ┌─────────────┐
│   Client    │ ──────────────→ │   Gateway   │ ──Operations──→ │ All Services│
└─────────────┘                 └─────────────┘                 └─────────────┘

Scope-Based (Fine-Grained):
┌─────────────┐  product:read    ┌─────────────┐  product:read   ┌─────────────┐
│   Client    │ ──cart:manage──→ │   Gateway   │ ──cart:manage─→ │   Specific  │
│             │  order:write     │             │  order:write    │  Operations │
└─────────────┘                 └─────────────┘                 └─────────────┘
```

**Trade-offs Analysis**:

| Aspect | Role-Based | Scope-Based ✅ |
|--------|------------|----------------|
| **Granularity** | ❌ Coarse (all or nothing) | ✅ Fine-grained (specific actions) |
| **Security** | ❌ Over-privileged users | ✅ Principle of least privilege |
| **Scalability** | ❌ Role explosion problem | ✅ Composable permissions |
| **Service Autonomy** | ❌ Cross-service role dependencies | ✅ Service-owned scopes |
| **Complexity** | ✅ Simple to implement | ❌ More complex configuration |
| **Standards Compliance** | ❌ Custom approach | ✅ OAuth2 standard |

**Interview Gold**: "We chose scope-based authorization for **security** and **scalability**. While more complex initially, it prevents **privilege escalation** and supports **service autonomy**."

### 2. **Scope Naming Convention Strategy**

**Service:Action Pattern**:
```yaml
# Our Scope Design
scopes:
  product:read      # Read access to product catalog
  cart:manage       # Full CRUD on user's cart
  order:write       # Create and modify orders
  payment:process   # Process payment transactions
  admin:all         # Administrative access across services
```

**Alternative Patterns Considered**:
```yaml
# Resource-based (rejected - too granular)
scopes:
  product.catalog.read
  product.inventory.write
  cart.items.create
  cart.items.delete

# Action-based (rejected - service coupling)
scopes:
  read
  write
  delete
  admin

# Hierarchical (rejected - complexity)
scopes:
  ecommerce.product.catalog.read
  ecommerce.cart.items.manage
```

**Why Service:Action Won**:
- ✅ **Clear Boundaries**: Each service owns its scopes
- ✅ **Intuitive**: Easy to understand what each scope allows
- ✅ **Scalable**: New services can define their own scopes
- ✅ **OAuth2 Compliant**: Follows OAuth2 scope conventions

---

## 🔧 **Technical Implementation Deep Dive**

### 1. **Keycloak Client Scopes Configuration**

**Interviewer**: *"Walk me through how you configure OAuth2 scopes in Keycloak and map them to users."*

**Client Scope Definition**:
```json
{
  "clientScopes": [
    {
      "name": "product:read",
      "description": "Read access to product catalog and information",
      "protocol": "openid-connect",
      "attributes": {
        "include.in.token.scope": "true",
        "display.on.consent.screen": "true",
        "consent.screen.text": "View products and catalog"
      },
      "protocolMappers": [
        {
          "name": "product-read-audience",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-audience-mapper",
          "config": {
            "included.client.audience": "product-service",
            "id.token.claim": "false",
            "access.token.claim": "true"
          }
        }
      ]
    },
    {
      "name": "cart:manage",
      "description": "Full access to shopping cart operations",
      "protocol": "openid-connect",
      "attributes": {
        "include.in.token.scope": "true",
        "display.on.consent.screen": "true",
        "consent.screen.text": "Manage your shopping cart"
      },
      "protocolMappers": [
        {
          "name": "cart-manage-audience",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-audience-mapper",
          "config": {
            "included.client.audience": "cart-service",
            "id.token.claim": "false",
            "access.token.claim": "true"
          }
        }
      ]
    }
  ]
}
```

**User Scope Assignment**:
```json
{
  "users": [
    {
      "username": "demo",
      "enabled": true,
      "realmRoles": ["ROLE_USER"],
      "clientRoles": {
        "react-spa": ["product:read", "cart:manage", "order:write"]
      }
    },
    {
      "username": "admin",
      "enabled": true,
      "realmRoles": ["ROLE_ADMIN"],
      "clientRoles": {
        "react-spa": ["product:read", "cart:manage", "order:write", "payment:process", "admin:all"]
      }
    }
  ]
}
```

**Key Configuration Features**:
- **Audience Mapping**: Scopes include target service audiences
- **Consent Screen**: Users see what permissions they're granting
- **Token Inclusion**: Scopes are included in access tokens
- **Service Targeting**: Each scope maps to specific services

### 2. **Custom Spring Security Expression Handler**

```java
// Custom Method Security Expression Root
public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot 
        implements MethodSecurityExpressionOperations {
    
    private final ScopeValidator scopeValidator;
    private Object filterObject;
    private Object returnObject;
    
    public CustomMethodSecurityExpressionRoot(Authentication authentication, 
                                            ScopeValidator scopeValidator) {
        super(authentication);
        this.scopeValidator = scopeValidator;
    }
    
    // Core scope validation method
    public boolean hasScope(String requiredScope) {
        return scopeValidator.hasScope(requiredScope);
    }
    
    // Multiple scope validation (OR logic)
    public boolean hasAnyScope(String... requiredScopes) {
        return scopeValidator.hasAnyScope(requiredScopes);
    }
    
    // Multiple scope validation (AND logic)
    public boolean hasAllScopes(String... requiredScopes) {
        return scopeValidator.hasAllScopes(requiredScopes);
    }
    
    // Service-specific convenience methods
    public boolean canReadProducts() {
        return hasScope("product:read");
    }
    
    public boolean canManageCart() {
        return hasScope("cart:manage");
    }
    
    public boolean canWriteOrders() {
        return hasScope("order:write");
    }
    
    public boolean canProcessPayments() {
        return hasScope("payment:process");
    }
    
    public boolean isServiceAdmin() {
        return hasScope("admin:all");
    }
    
    // Complex authorization logic
    public boolean canAccessResource(String resourceType, String action) {
        String requiredScope = resourceType + ":" + action;
        return hasScope(requiredScope) || hasScope("admin:all");
    }
    
    // Conditional scope checking
    public boolean canPerformAction(String action, Object resource) {
        // Business logic can be embedded in security expressions
        if (resource instanceof HighValueResource) {
            return hasScope("admin:all");
        }
        
        return hasScope("user:" + action);
    }
}
```

**Expression Handler Configuration**:
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class ScopeMethodSecurityConfig {
    
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            ScopeValidator scopeValidator) {
        
        CustomMethodSecurityExpressionHandler handler = 
            new CustomMethodSecurityExpressionHandler(scopeValidator);
        
        handler.setPermissionEvaluator(new ScopePermissionEvaluator(scopeValidator));
        return handler;
    }
}

// Custom Expression Handler
public class CustomMethodSecurityExpressionHandler 
        extends DefaultMethodSecurityExpressionHandler {
    
    private final ScopeValidator scopeValidator;
    
    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        
        CustomMethodSecurityExpressionRoot root = 
            new CustomMethodSecurityExpressionRoot(authentication, scopeValidator);
        
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(getRoleHierarchy());
        
        return root;
    }
}
```

### 3. **Scope Validation Utility**

```java
@Component
public class ScopeValidator {
    
    private final JwtProperties jwtProperties;
    
    // Extract scopes from current JWT token
    public Set<String> getUserScopes() {
        return getJwt()
            .map(jwt -> {
                String scopeClaim = jwt.getClaimAsString("scope");
                if (scopeClaim != null) {
                    return Arrays.stream(scopeClaim.split(" "))
                        .collect(Collectors.toSet());
                }
                return Collections.<String>emptySet();
            })
            .orElse(Collections.emptySet());
    }
    
    // Validate single scope
    public boolean hasScope(String requiredScope) {
        Set<String> userScopes = getUserScopes();
        
        // Direct scope match
        if (userScopes.contains(requiredScope)) {
            return true;
        }
        
        // Admin scope overrides all
        if (userScopes.contains(jwtProperties.getScopes().getAdminAll())) {
            return true;
        }
        
        return false;
    }
    
    // Validate multiple scopes (OR logic)
    public boolean hasAnyScope(String... requiredScopes) {
        return Arrays.stream(requiredScopes)
            .anyMatch(this::hasScope);
    }
    
    // Validate multiple scopes (AND logic)
    public boolean hasAllScopes(String... requiredScopes) {
        return Arrays.stream(requiredScopes)
            .allMatch(this::hasScope);
    }
    
    // Service-specific validation methods
    public boolean hasProductReadScope() {
        return hasScope(jwtProperties.getScopes().getProductRead());
    }
    
    public boolean hasCartManageScope() {
        return hasScope(jwtProperties.getScopes().getCartManage());
    }
    
    public boolean hasOrderWriteScope() {
        return hasScope(jwtProperties.getScopes().getOrderWrite());
    }
    
    public boolean hasPaymentProcessScope() {
        return hasScope(jwtProperties.getScopes().getPaymentProcess());
    }
    
    // Audience validation
    public boolean hasAudienceForService(String serviceName) {
        return getJwt()
            .map(jwt -> {
                List<String> audiences = jwt.getAudience();
                return audiences != null && audiences.contains(serviceName);
            })
            .orElse(false);
    }
    
    // Combined scope and audience validation
    public boolean canAccessService(String serviceName, String requiredScope) {
        return hasScope(requiredScope) && hasAudienceForService(serviceName);
    }
    
    private Optional<Jwt> getJwt() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(JwtAuthenticationToken.class::cast)
            .map(JwtAuthenticationToken::getToken);
    }
}
```

### 4. **Externalized Scope Configuration**

**Configuration Properties Class**:
```java
@ConfigurationProperties(prefix = "app.security.jwt")
@Data
public class JwtProperties {
    
    private String issuer;
    private String audience;
    private Claims claims = new Claims();
    private Scopes scopes = new Scopes();
    
    @Data
    public static class Claims {
        private String roles = "realm_access.roles";
        private String username = "preferred_username";
        private String email = "email";
        private String firstName = "given_name";
        private String lastName = "family_name";
    }
    
    @Data
    public static class Scopes {
        private String productRead = "product:read";
        private String cartManage = "cart:manage";
        private String orderWrite = "order:write";
        private String paymentProcess = "payment:process";
        private String adminAll = "admin:all";
    }
}
```

**YAML Configuration**:
```yaml
app:
  security:
    jwt:
      issuer: "${KC_ISSUER:http://localhost:8081/realms/ecommerce}"
      audience: "${KC_AUDIENCE:product-service}"
      claims:
        roles: "realm_access.roles"
        username: "preferred_username"
        email: "email"
        first-name: "given_name"
        last-name: "family_name"
      scopes:
        product-read: "product:read"
        cart-manage: "cart:manage"
        order-write: "order:write"
        payment-process: "payment:process"
        admin-all: "admin:all"
```

**Benefits of Externalized Configuration**:
- ✅ **Environment-Specific**: Different scopes per environment
- ✅ **No Hardcoded Values**: Follows project convention [[memory:7675571]]
- ✅ **Type Safety**: Spring Boot configuration properties
- ✅ **Maintainability**: Easy to update scope names

---

## 🚀 **Production Considerations - Advanced Topics**

### 1. **Dynamic Scope Management**

**Runtime Scope Updates**:
```java
@Component
public class DynamicScopeManager {
    
    private final ScopeRepository scopeRepository;
    private final ScopeCache scopeCache;
    private final ApplicationEventPublisher eventPublisher;
    
    @EventListener
    public void handleScopeUpdate(ScopeUpdateEvent event) {
        ScopeDefinition newScope = event.getScopeDefinition();
        
        // Validate scope definition
        if (validateScopeDefinition(newScope)) {
            // Update cache
            scopeCache.updateScope(newScope);
            
            // Persist to database
            scopeRepository.save(newScope);
            
            // Notify other service instances
            eventPublisher.publishEvent(new ScopeCacheInvalidationEvent(newScope.getName()));
            
            log.info("Scope updated: {}", newScope.getName());
        } else {
            log.error("Invalid scope definition rejected: {}", newScope);
        }
    }
    
    public List<ScopeDefinition> getAvailableScopes(String serviceName) {
        return scopeCache.getScopesByService(serviceName);
    }
    
    public boolean isScopeValid(String scopeName) {
        return scopeCache.hasScope(scopeName);
    }
    
    private boolean validateScopeDefinition(ScopeDefinition scope) {
        // Validate scope name format
        if (!scope.getName().matches("^[a-z]+:[a-z]+$")) {
            return false;
        }
        
        // Validate service ownership
        String serviceName = scope.getName().split(":")[0];
        if (!serviceRegistry.isValidService(serviceName)) {
            return false;
        }
        
        // Validate permissions
        return scope.getPermissions().stream()
            .allMatch(this::isValidPermission);
    }
}
```

### 2. **Scope Hierarchy and Inheritance**

```java
// Hierarchical scope validation
@Component
public class HierarchicalScopeValidator extends ScopeValidator {
    
    private final ScopeHierarchy scopeHierarchy;
    
    @Override
    public boolean hasScope(String requiredScope) {
        Set<String> userScopes = getUserScopes();
        
        // Direct scope match
        if (userScopes.contains(requiredScope)) {
            return true;
        }
        
        // Check hierarchical scopes
        return userScopes.stream()
            .anyMatch(userScope -> scopeHierarchy.implies(userScope, requiredScope));
    }
    
    // Scope hierarchy configuration
    @Bean
    public ScopeHierarchy scopeHierarchy() {
        ScopeHierarchyImpl hierarchy = new ScopeHierarchyImpl();
        
        // Admin scopes imply all other scopes
        hierarchy.setHierarchy(
            "admin:all > product:write > product:read\n" +
            "admin:all > cart:manage > cart:read\n" +
            "admin:all > order:write > order:read\n" +
            "admin:all > payment:process > payment:read"
        );
        
        return hierarchy;
    }
}

// Custom scope hierarchy implementation
public class ScopeHierarchyImpl implements ScopeHierarchy {
    
    private final Map<String, Set<String>> scopeHierarchy = new HashMap<>();
    
    public void setHierarchy(String hierarchyDefinition) {
        String[] lines = hierarchyDefinition.split("\n");
        
        for (String line : lines) {
            String[] scopes = line.split(" > ");
            
            for (int i = 0; i < scopes.length - 1; i++) {
                String parentScope = scopes[i].trim();
                String childScope = scopes[i + 1].trim();
                
                scopeHierarchy.computeIfAbsent(parentScope, k -> new HashSet<>())
                    .add(childScope);
            }
        }
    }
    
    @Override
    public boolean implies(String parentScope, String childScope) {
        if (parentScope.equals(childScope)) {
            return true;
        }
        
        Set<String> impliedScopes = scopeHierarchy.get(parentScope);
        if (impliedScopes == null) {
            return false;
        }
        
        if (impliedScopes.contains(childScope)) {
            return true;
        }
        
        // Recursive check for transitive implications
        return impliedScopes.stream()
            .anyMatch(impliedScope -> implies(impliedScope, childScope));
    }
}
```

### 3. **Scope-Based Rate Limiting**

```java
// Scope-aware rate limiting
@Component
public class ScopeBasedRateLimiter {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ScopeValidator scopeValidator;
    
    public boolean isAllowed(String userId, String endpoint) {
        Set<String> userScopes = scopeValidator.getUserScopes();
        
        // Different rate limits based on scopes
        RateLimitConfig config = getRateLimitConfig(userScopes, endpoint);
        
        String key = String.format("rate_limit:%s:%s", userId, config.getScope());
        
        return checkRateLimit(key, config.getLimit(), config.getWindow());
    }
    
    private RateLimitConfig getRateLimitConfig(Set<String> userScopes, String endpoint) {
        // Admin users get higher limits
        if (userScopes.contains("admin:all")) {
            return RateLimitConfig.builder()
                .scope("admin")
                .limit(1000)
                .window(Duration.ofMinutes(1))
                .build();
        }
        
        // Premium users (multiple scopes) get higher limits
        if (userScopes.size() > 3) {
            return RateLimitConfig.builder()
                .scope("premium")
                .limit(500)
                .window(Duration.ofMinutes(1))
                .build();
        }
        
        // Regular users get standard limits
        return RateLimitConfig.builder()
            .scope("standard")
            .limit(100)
            .window(Duration.ofMinutes(1))
            .build();
    }
    
    private boolean checkRateLimit(String key, int limit, Duration window) {
        String luaScript = 
            "local current = redis.call('GET', KEYS[1]) or 0 " +
            "if tonumber(current) < tonumber(ARGV[1]) then " +
            "  redis.call('INCR', KEYS[1]) " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            RedisScript.of(luaScript, Long.class),
            Collections.singletonList(key),
            String.valueOf(limit),
            String.valueOf(window.getSeconds())
        );
        
        return result != null && result == 1;
    }
}
```

---

## 🎯 **Interview Questions You Can Now Answer**

### 1. **System Design Question**
*"Design a fine-grained authorization system for a microservices platform with 100+ services and complex permission requirements"*

**Structured Answer Framework**:

1. **Requirements Analysis**:
   - Services: 100+ microservices with different authorization needs
   - Users: 10K+ users with varying permission sets
   - Permissions: Fine-grained, service-specific, action-based
   - Performance: <2ms authorization overhead per request

2. **High-Level Architecture**:
   ```
   [Identity Provider] → [Scope Management] → [Policy Engine] → [Services]
           │                    │                   │              │
           ▼                    ▼                   ▼              ▼
   [User/Scope Mapping] → [Dynamic Scopes] → [Authorization] → [Business Logic]
   ```

3. **Scope Design Strategy**:
   - **Naming Convention**: `service:action` pattern
   - **Hierarchy**: Admin scopes imply lower-level scopes
   - **Composition**: Users can have multiple scopes
   - **Service Ownership**: Each service defines its own scopes

4. **Implementation Components**:
   - **Scope Registry**: Central registry of all available scopes
   - **Dynamic Management**: Runtime scope updates without deployment
   - **Validation Engine**: Custom Spring Security expressions
   - **Caching Layer**: Redis-based scope validation caching

5. **Scalability Considerations**:
   - **Scope Caching**: Cache user scopes for performance
   - **Lazy Loading**: Load scopes on-demand
   - **Batch Validation**: Validate multiple scopes in single call
   - **Distributed Cache**: Shared scope cache across instances

### 2. **Deep Technical Question**
*"How do you implement custom Spring Security expressions for complex authorization logic while maintaining performance?"*

**Comprehensive Implementation**:

```java
// High-performance custom expression evaluator
@Component
public class OptimizedScopeExpressionEvaluator {
    
    private final LoadingCache<String, CompiledExpression> expressionCache;
    private final ScopeValidator scopeValidator;
    
    public OptimizedScopeExpressionEvaluator(ScopeValidator scopeValidator) {
        this.scopeValidator = scopeValidator;
        this.expressionCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(this::compileExpression);
    }
    
    public boolean evaluate(String expression, Authentication authentication) {
        try {
            CompiledExpression compiled = expressionCache.get(expression);
            
            // Create evaluation context
            EvaluationContext context = createEvaluationContext(authentication);
            
            // Evaluate expression
            return compiled.getValue(context, Boolean.class);
            
        } catch (Exception ex) {
            log.error("Expression evaluation failed: {}", expression, ex);
            return false; // Fail securely
        }
    }
    
    private CompiledExpression compileExpression(String expression) {
        SpelExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression(expression);
    }
    
    private EvaluationContext createEvaluationContext(Authentication authentication) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // Register custom functions
        context.registerFunction("hasScope", 
            ReflectionUtils.findMethod(ScopeValidator.class, "hasScope", String.class));
        context.registerFunction("hasAnyScope", 
            ReflectionUtils.findMethod(ScopeValidator.class, "hasAnyScope", String[].class));
        context.registerFunction("hasAllScopes", 
            ReflectionUtils.findMethod(ScopeValidator.class, "hasAllScopes", String[].class));
        
        // Set variables
        context.setVariable("scopeValidator", scopeValidator);
        context.setVariable("authentication", authentication);
        context.setVariable("principal", authentication.getPrincipal());
        
        return context;
    }
}

// Usage in method security
@RestController
public class ProductController {
    
    // Simple scope check
    @GetMapping("/products")
    @PreAuthorize("hasScope('product:read')")
    public List<Product> getProducts() { }
    
    // Complex authorization logic
    @PostMapping("/products")
    @PreAuthorize("hasScope('product:write') and " +
                  "(hasScope('admin:all') or @productService.canUserCreateProduct(authentication.name))")
    public Product createProduct(@RequestBody Product product) { }
    
    // Dynamic scope validation
    @PutMapping("/products/{id}")
    @PreAuthorize("@scopeValidator.canAccessResource('product', 'write', #id, authentication)")
    public Product updateProduct(@PathVariable String id, @RequestBody Product product) { }
}
```

**Performance Optimizations**:
- **Expression Caching**: Compiled expressions cached for reuse
- **Lazy Evaluation**: Short-circuit evaluation for complex expressions
- **Batch Validation**: Multiple scope checks in single operation
- **Context Reuse**: Evaluation context pooling

### 3. **Security Architecture Question**
*"How do you handle scope evolution and backward compatibility in a large-scale system?"*

**Scope Evolution Strategy**:

```java
// Versioned scope management
@Component
public class VersionedScopeManager {
    
    private final ScopeVersionRegistry versionRegistry;
    
    public boolean validateScope(String scopeName, String apiVersion) {
        ScopeVersion scopeVersion = versionRegistry.getScopeVersion(scopeName, apiVersion);
        
        if (scopeVersion == null) {
            // Check for backward compatibility
            return checkBackwardCompatibility(scopeName, apiVersion);
        }
        
        return scopeVersion.isActive();
    }
    
    private boolean checkBackwardCompatibility(String scopeName, String apiVersion) {
        // Check if scope was renamed or merged
        ScopeMigration migration = versionRegistry.getMigration(scopeName, apiVersion);
        
        if (migration != null) {
            log.warn("Using deprecated scope '{}' in API version '{}'. " +
                    "Consider migrating to '{}'", 
                    scopeName, apiVersion, migration.getNewScopeName());
            
            return validateScope(migration.getNewScopeName(), migration.getTargetVersion());
        }
        
        return false;
    }
    
    // Scope migration configuration
    @Bean
    public ScopeVersionRegistry scopeVersionRegistry() {
        ScopeVersionRegistry registry = new ScopeVersionRegistry();
        
        // Define scope migrations
        registry.addMigration(ScopeMigration.builder()
            .oldScope("product:manage")
            .newScope("product:write")
            .fromVersion("v1")
            .toVersion("v2")
            .deprecationDate(LocalDate.of(2024, 1, 1))
            .removalDate(LocalDate.of(2024, 6, 1))
            .build());
        
        return registry;
    }
}

// Backward compatibility filter
@Component
public class ScopeCompatibilityFilter implements Filter {
    
    private final VersionedScopeManager scopeManager;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String apiVersion = extractApiVersion(httpRequest);
        
        // Check for deprecated scopes
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
            Set<String> userScopes = extractScopes(jwtAuth.getToken());
            
            for (String scope : userScopes) {
                if (scopeManager.isDeprecated(scope, apiVersion)) {
                    // Add warning header
                    ((HttpServletResponse) response).addHeader(
                        "X-Deprecated-Scope", 
                        String.format("Scope '%s' is deprecated. Use '%s' instead.", 
                            scope, scopeManager.getReplacementScope(scope))
                    );
                }
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

**Migration Strategies**:
- **Gradual Migration**: Support both old and new scopes during transition
- **Deprecation Warnings**: Clear communication about scope changes
- **Version-Aware Validation**: Different scope rules per API version
- **Automated Migration**: Tools to help users migrate to new scopes

---

## 🔥 **Advanced Topics for Senior Interviews**

### 1. **Conditional Scopes and Context-Aware Authorization**

```java
// Context-aware scope validation
@Component
public class ContextAwareScopeValidator extends ScopeValidator {
    
    private final GeolocationService geolocationService;
    private final TimeBasedAccessService timeBasedAccessService;
    
    public boolean hasContextualScope(String requiredScope, AuthorizationContext context) {
        // Basic scope check
        if (!hasScope(requiredScope)) {
            return false;
        }
        
        // Geographic restrictions
        if (hasGeographicRestrictions(requiredScope)) {
            String userLocation = geolocationService.getUserLocation(context.getClientIp());
            if (!isAllowedLocation(requiredScope, userLocation)) {
                log.warn("Access denied due to geographic restrictions: scope={}, location={}", 
                    requiredScope, userLocation);
                return false;
            }
        }
        
        // Time-based restrictions
        if (hasTimeRestrictions(requiredScope)) {
            if (!timeBasedAccessService.isAccessAllowed(requiredScope, context.getTimestamp())) {
                log.warn("Access denied due to time restrictions: scope={}, time={}", 
                    requiredScope, context.getTimestamp());
                return false;
            }
        }
        
        // Risk-based restrictions
        if (isHighRiskOperation(requiredScope)) {
            RiskScore riskScore = calculateRiskScore(context);
            if (riskScore.getScore() > getRiskThreshold(requiredScope)) {
                log.warn("Access denied due to high risk score: scope={}, risk={}", 
                    requiredScope, riskScore.getScore());
                return false;
            }
        }
        
        return true;
    }
    
    private RiskScore calculateRiskScore(AuthorizationContext context) {
        RiskScoreBuilder builder = RiskScore.builder();
        
        // Device risk
        if (context.isUnknownDevice()) {
            builder.addRisk("unknown_device", 20);
        }
        
        // Location risk
        if (context.isUnusualLocation()) {
            builder.addRisk("unusual_location", 30);
        }
        
        // Time risk
        if (context.isUnusualTime()) {
            builder.addRisk("unusual_time", 10);
        }
        
        // Velocity risk
        if (context.hasHighVelocity()) {
            builder.addRisk("high_velocity", 25);
        }
        
        return builder.build();
    }
}
```

### 2. **Scope-Based Data Filtering**

```java
// Data filtering based on scopes
@Component
public class ScopeBasedDataFilter {
    
    private final ScopeValidator scopeValidator;
    
    public <T> List<T> filterData(List<T> data, Class<T> entityClass) {
        Set<String> userScopes = scopeValidator.getUserScopes();
        
        return data.stream()
            .filter(item -> canAccessData(item, entityClass, userScopes))
            .map(item -> maskSensitiveFields(item, entityClass, userScopes))
            .collect(Collectors.toList());
    }
    
    private <T> boolean canAccessData(T item, Class<T> entityClass, Set<String> userScopes) {
        // Check entity-level access
        ScopeRequired entityScope = entityClass.getAnnotation(ScopeRequired.class);
        if (entityScope != null) {
            if (!userScopes.contains(entityScope.value())) {
                return false;
            }
        }
        
        // Check field-level access
        return Arrays.stream(entityClass.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(ScopeRequired.class))
            .allMatch(field -> {
                ScopeRequired fieldScope = field.getAnnotation(ScopeRequired.class);
                return userScopes.contains(fieldScope.value());
            });
    }
    
    private <T> T maskSensitiveFields(T item, Class<T> entityClass, Set<String> userScopes) {
        try {
            T maskedItem = (T) entityClass.getDeclaredConstructor().newInstance();
            
            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);
                
                ScopeRequired scopeRequired = field.getAnnotation(ScopeRequired.class);
                if (scopeRequired != null && !userScopes.contains(scopeRequired.value())) {
                    // Mask sensitive field
                    field.set(maskedItem, getMaskedValue(field.getType()));
                } else {
                    // Copy field value
                    field.set(maskedItem, field.get(item));
                }
            }
            
            return maskedItem;
        } catch (Exception ex) {
            log.error("Failed to mask sensitive fields", ex);
            return item;
        }
    }
}

// Usage with annotations
@Entity
@ScopeRequired("user:profile:read")
public class UserProfile {
    
    private String id;
    
    private String username;
    
    @ScopeRequired("user:profile:email:read")
    private String email;
    
    @ScopeRequired("user:profile:phone:read")
    private String phoneNumber;
    
    @ScopeRequired("admin:user:pii:read")
    private String socialSecurityNumber;
}
```

### 3. **Scope Analytics and Monitoring**

```java
// Scope usage analytics
@Component
public class ScopeAnalyticsService {
    
    private final MeterRegistry meterRegistry;
    private final ScopeUsageRepository usageRepository;
    
    @EventListener
    public void handleScopeUsage(ScopeUsageEvent event) {
        // Record metrics
        recordScopeUsageMetrics(event);
        
        // Store detailed usage data
        storeScopeUsageData(event);
        
        // Check for anomalies
        checkForAnomalies(event);
    }
    
    private void recordScopeUsageMetrics(ScopeUsageEvent event) {
        // Scope usage counter
        Counter.builder("scope.usage")
            .tag("scope", event.getScopeName())
            .tag("service", event.getServiceName())
            .tag("user_type", event.getUserType())
            .register(meterRegistry)
            .increment();
        
        // Scope validation time
        Timer.builder("scope.validation.duration")
            .tag("scope", event.getScopeName())
            .register(meterRegistry)
            .record(event.getValidationDuration());
        
        // Failed scope checks
        if (!event.isAccessGranted()) {
            Counter.builder("scope.access.denied")
                .tag("scope", event.getScopeName())
                .tag("reason", event.getDenialReason())
                .register(meterRegistry)
                .increment();
        }
    }
    
    private void checkForAnomalies(ScopeUsageEvent event) {
        // Unusual scope combinations
        if (hasUnusualScopeCombination(event.getUserScopes())) {
            alertingService.sendAlert(SecurityAlert.builder()
                .type("UNUSUAL_SCOPE_COMBINATION")
                .userId(event.getUserId())
                .scopes(event.getUserScopes())
                .timestamp(event.getTimestamp())
                .build());
        }
        
        // High-privilege scope usage
        if (isHighPrivilegeScope(event.getScopeName()) && 
            !isExpectedHighPrivilegeUser(event.getUserId())) {
            alertingService.sendAlert(SecurityAlert.builder()
                .type("UNEXPECTED_HIGH_PRIVILEGE_ACCESS")
                .userId(event.getUserId())
                .scope(event.getScopeName())
                .timestamp(event.getTimestamp())
                .build());
        }
    }
    
    public ScopeUsageReport generateUsageReport(String serviceName, LocalDate startDate, LocalDate endDate) {
        List<ScopeUsageData> usageData = usageRepository.findByServiceAndDateRange(
            serviceName, startDate, endDate);
        
        return ScopeUsageReport.builder()
            .serviceName(serviceName)
            .reportPeriod(DateRange.of(startDate, endDate))
            .totalRequests(usageData.size())
            .uniqueUsers(usageData.stream().map(ScopeUsageData::getUserId).distinct().count())
            .scopeBreakdown(calculateScopeBreakdown(usageData))
            .topUsers(findTopUsers(usageData))
            .anomalies(findAnomalies(usageData))
            .build();
    }
}
```

---

## 🎖️ **What This Demonstrates to Interviewers**

### Technical Skills ✅
- **OAuth2 Advanced**: Deep understanding of scope-based authorization
- **Spring Security**: Custom expression handlers and method security
- **Architecture Design**: Fine-grained authorization patterns
- **Performance Engineering**: Caching, optimization, scalability

### Engineering Maturity ✅
- **Security-First Design**: Principle of least privilege implementation
- **Standards Compliance**: OAuth2 scope standards adherence
- **Maintainability**: Externalized configuration, clear abstractions
- **Observability**: Comprehensive monitoring and analytics

### Real-world Experience ✅
- **Enterprise Scale**: Handling complex authorization requirements
- **Evolution Management**: Scope versioning and backward compatibility
- **Production Operations**: Monitoring, alerting, performance optimization
- **Team Collaboration**: Clear documentation, testing frameworks

---

## 🚀 **Key Takeaways for Interviews**

### 1. **Fine-Grained Authorization is Essential**
Move beyond simple roles to action-based, service-specific permissions.

### 2. **Standards-Based Approach**
Use OAuth2 scopes rather than custom authorization schemes.

### 3. **Service Autonomy with Consistency**
Each service owns its scopes while maintaining consistent patterns.

### 4. **Performance and Scalability**
Cache scope validations and optimize for high-throughput scenarios.

### 5. **Evolution and Backward Compatibility**
Plan for scope changes and provide migration paths.

---

## 📚 **Further Learning & Related Topics**

### Next Level Topics:
- **Attribute-Based Access Control (ABAC)**: Context-aware authorization
- **Policy as Code**: Open Policy Agent (OPA) integration
- **Zero Trust Architecture**: Never trust, always verify
- **Dynamic Authorization**: Runtime policy updates

### Related System Design Questions:
- Design authorization for a multi-tenant SaaS platform
- Design fine-grained permissions for a document management system
- Design authorization for a global financial services platform
- Design scope-based API rate limiting system

---

**Remember**: This implementation showcases **enterprise-grade fine-grained authorization** - exactly what principal/staff engineering interviews are looking for! 🎯
