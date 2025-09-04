# 🚀 Performance & Scalability - Interview Guide

**Project**: Mambogo E-commerce Microservices MVP  
**Focus**: Performance Optimization, Horizontal Scaling, Load Testing, Monitoring  
**Level**: Senior Performance Engineer / SRE  
**Date**: January 2025  

---

## 🏗️ Performance Architecture Overview

### Performance Optimization Layers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  React SPA (Vite)                                                         │
│  • Code Splitting & Lazy Loading                                          │
│  • Service Worker Caching                                                 │
│  • Bundle Optimization                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ CDN + Load Balancer
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        API GATEWAY LAYER                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  Spring Cloud Gateway (Port 8080)                                         │
│  • Rate Limiting (Redis-based)                                           │
│  • Request/Response Caching                                               │
│  • Circuit Breakers                                                       │
│  • Connection Pooling                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Service Discovery + Load Balancing
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MICROSERVICES LAYER                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │
│  │  Product   │ │    Cart     │ │    Order    │ │   Payment   │         │
│  │  Service   │ │   Service   │ │   Service   │ │   Service   │         │
│  │ • Caching  │ │ • Redis     │ │ • Async     │ │ • Circuit   │         │
│  │ • Indexing │ │ • TTL       │ │ • Batching  │ │   Breaker   │         │
│  │ • Pooling  │ │ • Clustering│ │ • Pooling   │ │ • Pooling   │         │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Performance Optimization Strategies

### **1. Application-Level Caching**

#### **Multi-Layer Caching Strategy**

```java
@Service
public class ProductService {
    
    // L1: Application Cache (Caffeine)
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Product getProduct(UUID id) {
        // L2: Redis Cache
        Product cachedProduct = redisTemplate.opsForValue().get("product:" + id);
        if (cachedProduct != null) {
            return cachedProduct;
        }
        
        // L3: Database
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        // Cache in Redis with TTL
        redisTemplate.opsForValue().set("product:" + id, product, Duration.ofMinutes(30));
        
        return product;
    }
}

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

### **2. Database Performance Optimization**

#### **Connection Pooling Configuration**

```yaml
# Database performance configuration
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
```

#### **Query Optimization with Indexing**

```sql
-- Product Service Performance Indexes
CREATE INDEX CONCURRENTLY idx_products_active_category ON products(is_active, category_id) 
    WHERE is_active = TRUE;

CREATE INDEX CONCURRENTLY idx_products_price_stock ON products(price, stock_quantity) 
    WHERE is_active = TRUE AND stock_quantity > 0;

-- Order Service Performance Indexes
CREATE INDEX CONCURRENTLY idx_orders_user_status ON orders(user_id, status, created_at);

CREATE INDEX CONCURRENTLY idx_orders_pending_expired ON orders(id, status, expires_at) 
    WHERE status = 'PENDING';
```

### **3. Async Processing & Non-Blocking Operations**

#### **Async Service Implementation**

```java
@Service
public class OrderProcessingService {
    
    @Async("orderProcessingExecutor")
    public CompletableFuture<OrderProcessingResult> processOrderAsync(Order order) {
        try {
            // Process order asynchronously
            OrderProcessingResult result = processOrder(order);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "orderProcessingExecutor")
    public Executor orderProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OrderProcessor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 📈 Horizontal Scaling Strategies

### **1. Kubernetes Horizontal Scaling**

#### **Deployment Configuration**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: order-service
        image: mambogo/order-service:latest
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

#### **Horizontal Pod Autoscaler**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### **2. Database Scaling Patterns**

#### **Read Replica Configuration**

```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.write")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.read")
    public DataSource readDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public DataSource routingDataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
            "WRITE", writeDataSource(),
            "READ", readDataSource()
        ));
        routingDataSource.setDefaultTargetDataSource(writeDataSource());
        return routingDataSource;
    }
}

@Component
public class DatabaseRoutingAspect {
    
    @Around("@annotation(ReadOnly)")
    public Object routeToReadReplica(ProceedingJoinPoint joinPoint) throws Throwable {
        DatabaseContextHolder.setDataSourceType("READ");
        try {
            return joinPoint.proceed();
        } finally {
            DatabaseContextHolder.clearDataSourceType();
        }
    }
}
```

---

## 🔄 Load Balancing & Circuit Breakers

### **1. Spring Cloud Load Balancer**

#### **Load Balancer Configuration**

```java
@Configuration
@LoadBalancerClient(name = "order-service", configuration = LoadBalancerConfig.class)
public class LoadBalancerConfig {
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(loadBalancerClientFactory
            .getLazyProvider(name, ServiceInstanceListSupplier.class),
            name);
    }
}
```

### **2. Circuit Breaker Implementation**

#### **Resilience4j Circuit Breaker**

```java
@Service
public class InventoryServiceClient {
    
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackReserveInventory")
    @Retry(name = "inventory-service", fallbackMethod = "fallbackReserveInventory")
    @TimeLimiter(name = "inventory-service")
    public CompletableFuture<InventoryReservationResponse> reserveInventoryAsync(
            UUID orderId, List<OrderItem> items) {
        
        return CompletableFuture.supplyAsync(() -> {
            return webClient.post()
                .uri("/api/inventory/reserve")
                .bodyValue(new InventoryReservationRequest(orderId, items))
                .retrieve()
                .bodyToMono(InventoryReservationResponse.class)
                .block();
        });
    }
    
    public InventoryReservationResponse fallbackReserveInventory(UUID orderId, 
                                                               List<OrderItem> items, 
                                                               Exception e) {
        log.warn("Inventory service unavailable, using fallback for order: {}", orderId);
        return InventoryReservationResponse.builder()
            .success(false)
            .message("Service temporarily unavailable")
            .fallbackUsed(true)
            .build();
    }
}
```

---

## 📊 Performance Monitoring & Metrics

### **1. Micrometer Metrics Collection**

#### **Custom Metrics Implementation**

```java
@Component
public class PerformanceMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter ordersCreated;
    private final Counter ordersProcessed;
    private final Counter ordersFailed;
    
    // Timers
    private final Timer orderProcessingTime;
    private final Timer inventoryReservationTime;
    private final Timer paymentProcessingTime;
    
    public PerformanceMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.ordersCreated = Counter.builder("orders.created")
            .description("Number of orders created")
            .register(meterRegistry);
            
        this.ordersProcessed = Counter.builder("orders.processed")
            .description("Number of orders successfully processed")
            .register(meterRegistry);
            
        this.ordersFailed = Counter.builder("orders.failed")
            .description("Number of orders that failed processing")
            .register(meterRegistry);
        
        // Initialize timers
        this.orderProcessingTime = Timer.builder("orders.processing.time")
            .description("Time taken to process orders")
            .register(meterRegistry);
            
        this.inventoryReservationTime = Timer.builder("inventory.reservation.time")
            .description("Time taken to reserve inventory")
            .register(meterRegistry);
            
        this.paymentProcessingTime = Timer.builder("payment.processing.time")
            .description("Time taken to process payments")
            .register(meterRegistry);
    }
    
    public void recordOrderCreated() {
        ordersCreated.increment();
    }
    
    public void recordOrderProcessed() {
        ordersProcessed.increment();
    }
    
    public void recordOrderFailed() {
        ordersFailed.increment();
    }
    
    public void recordOrderProcessingTime(Duration duration) {
        orderProcessingTime.record(duration);
    }
    
    public void recordInventoryReservationTime(Duration duration) {
        inventoryReservationTime.record(duration);
    }
    
    public void recordPaymentProcessingTime(Duration duration) {
        paymentProcessingTime.record(duration);
    }
}
```

### **2. Prometheus Configuration**

#### **Prometheus Metrics Endpoint**

```yaml
# Prometheus configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
        orders.processing.time: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
        orders.processing.time: 0.5,0.95,0.99
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

---

## 🧪 Load Testing & Performance Testing

### **1. JMeter Test Plan**

#### **Order Creation Load Test**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.5">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Order Service Load Test" enabled="true">
      <stringProp name="TestPlan.comments">Load test for order service with 100 concurrent users</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
          <elementProp name="AUTH_TOKEN" elementType="Argument">
            <stringProp name="Argument.name">AUTH_TOKEN</stringProp>
            <stringProp name="Argument.value">${__P(AUTH_TOKEN,)}</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Order Creation Load Test" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControllerGui" testclass="LoopController" testname="Loop Controller" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">10</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
        <stringProp name="ThreadGroup.duration"></stringProp>
        <stringProp name="ThreadGroup.delay"></stringProp>
        <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Create Order" enabled="true" domain="localhost" port="8080" protocol="http" path="/api/orders" method="POST" follow_redirects="true" auto_redirects="false" use_keepalive="true" DO_MULTIPART_POST="false">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
            <collectionProp name="Arguments.arguments">
              <elementProp name="idempotencyKey" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">${__UUID}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
                <boolProp name="HTTPArgument.use_equals">true</boolProp>
                <stringProp name="Argument.name">X-Idempotency-Key</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <elementProp name="HTTPsampler.Files" elementType="HTTPFileArgs" guiclass="HTTPFileArgsPanel" testclass="HTTPFileArgs" testname="HTTPsampler.Files" enabled="true">
            <collectionProp name="HTTPFileArgs.files"/>
          </elementProp>
          <stringProp name="HTTPsampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPsampler.port"></stringProp>
          <stringProp name="HTTPsampler.protocol"></stringProp>
          <stringProp name="HTTPsampler.contentEncoding"></stringProp>
          <stringProp name="HTTPsampler.path">/api/orders</stringProp>
          <stringProp name="HTTPsampler.method">POST</stringProp>
          <boolProp name="HTTPsampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPsampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPsampler.use_keepalive">true</boolProp>
          <boolProp name="HTTPsampler.DO_MULTIPART_POST">false</boolProp>
          <stringProp name="HTTPsampler.embedded_url_re"></stringProp>
          <stringProp name="HTTPsampler.connect_timeout"></stringProp>
          <stringProp name="HTTPsampler.response_timeout"></stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager" enabled="true">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="" elementType="Header">
                <stringProp name="Header.name">Content-Type</stringProp>
                <stringProp name="Header.value">application/json</stringProp>
              </elementProp>
              <elementProp name="" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${AUTH_TOKEN}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
          <hashTree/>
        </hashTree>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### **2. Performance Testing Scripts**

#### **Load Testing with Artillery**

```yaml
# artillery-load-test.yml
config:
  target: 'http://localhost:8080'
  phases:
    - duration: 60
      arrivalRate: 10
      name: "Warm up phase"
    - duration: 300
      arrivalRate: 50
      name: "Sustained load phase"
    - duration: 120
      arrivalRate: 100
      name: "Peak load phase"
  defaults:
    headers:
      Content-Type: 'application/json'
      Authorization: 'Bearer {{ $processEnvironment.AUTH_TOKEN }}'

scenarios:
  - name: "Order Creation Flow"
    weight: 70
    flow:
      - post:
          url: "/api/orders"
          headers:
            X-Idempotency-Key: "{{ $randomString() }}"
          json:
            items:
              - productId: "{{ $randomUUID() }}"
                quantity: "{{ $randomInt(1, 5) }}"
            shippingAddress: "123 Test St, Test City, TC 12345"
            paymentMethod: "CREDIT_CARD"
          capture:
            - json: "$.orderId"
              as: "orderId"
      - get:
          url: "/api/orders/{{ orderId }}"
          
  - name: "Product Browsing"
    weight: 30
    flow:
      - get:
          url: "/api/products"
      - get:
          url: "/api/products/{{ $randomUUID() }}"
```

---

## ❓ Common Performance Interview Questions

### **Q1: How do you identify performance bottlenecks?**

**Answer**: Multi-layered approach:
1. **Application Monitoring**: APM tools, custom metrics, profiling
2. **Database Analysis**: Query execution plans, slow query logs, connection pooling
3. **Infrastructure Monitoring**: CPU, memory, network, disk I/O
4. **Load Testing**: JMeter, Artillery, Gatling for stress testing
5. **Profiling**: JProfiler, YourKit, VisualVM for Java profiling

### **Q2: How do you scale a microservice to handle 10x load?**

**Answer**: Multi-dimensional scaling:
1. **Horizontal Scaling**: Multiple service instances with load balancing
2. **Database Scaling**: Read replicas, connection pooling, query optimization
3. **Caching Strategy**: Redis clustering, application-level caching
4. **Async Processing**: Non-blocking operations, event-driven architecture
5. **Resource Optimization**: JVM tuning, connection pooling, batch processing

### **Q3: How do you handle database performance issues?**

**Answer**: Systematic approach:
1. **Query Optimization**: Analyze execution plans, add proper indexes
2. **Connection Pooling**: Optimize HikariCP settings, monitor pool usage
3. **Read Replicas**: Distribute read load across multiple database instances
4. **Caching**: Redis for frequently accessed data, application-level caching
5. **Monitoring**: Track slow queries, connection usage, resource utilization

### **Q4: How do you ensure performance in production?**

**Answer**: Continuous monitoring and optimization:
1. **Real-time Monitoring**: Prometheus + Grafana for metrics visualization
2. **Alerting**: Set thresholds for response times, error rates, resource usage
3. **Performance Testing**: Regular load testing in staging environment
4. **Capacity Planning**: Monitor trends and plan for growth
5. **Optimization**: Continuous improvement based on metrics and feedback

---

## 🎯 Performance Assessment Summary

### **Performance Rating: 9.0/10**

| Aspect | Score | Implementation | Benefits |
|--------|-------|----------------|----------|
| **Caching Strategy** | 9/10 | Multi-layer (App + Redis) | Reduced database load |
| **Database Optimization** | 9/10 | Connection pooling + indexing | Faster query execution |
| **Async Processing** | 9/10 | CompletableFuture + ThreadPools | Non-blocking operations |
| **Horizontal Scaling** | 9/10 | Kubernetes HPA + Load balancing | Elastic scaling |
| **Monitoring** | 8/10 | Micrometer + Prometheus | Visibility into performance |

### **Key Strengths**
1. **Multi-Layer Caching**: Application + Redis for optimal performance
2. **Database Optimization**: Connection pooling, indexing, read replicas
3. **Async Processing**: Non-blocking operations with proper thread pools
4. **Horizontal Scaling**: Kubernetes-based auto-scaling
5. **Performance Monitoring**: Comprehensive metrics collection

### **Areas for Enhancement**
1. **CDN Integration**: Could add CDN for static content delivery
2. **Database Sharding**: Could implement horizontal database partitioning
3. **Advanced Caching**: Could implement distributed caching with Redis Cluster

---

*This guide covers performance optimization and scalability strategies. For detailed implementations, refer to the individual service logs.*
