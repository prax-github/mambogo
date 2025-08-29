# Kubernetes Optimization and Multi-Environment Development Plan

## Table of Contents
1. [Overview](#overview)
2. [Current State Analysis](#current-state-analysis)
3. [Recommended Changes](#recommended-changes)
4. [Implementation Phases](#implementation-phases)
5. [Configuration Management](#configuration-management)
6. [Service Discovery Strategy](#service-discovery-strategy)
7. [Multi-Environment Setup](#multi-environment-setup)
8. [Security Enhancements](#security-enhancements)
9. [Monitoring and Observability](#monitoring-and-observability)
10. [Testing Strategy](#testing-strategy)
11. [Deployment Strategy](#deployment-strategy)
12. [Rollback Plan](#rollback-plan)

## Overview

This document outlines the comprehensive plan to optimize the Mambogo e-commerce platform for Kubernetes deployment while maintaining seamless local development, testing, and demo environments. The plan focuses on making the application more Kubernetes-native, improving reliability, security, and operational efficiency.

## Current State Analysis

### ✅ What's Working Well
- **Microservices Architecture**: Well-structured with 5 services (order, payment, product, cart, gateway)
- **Event-Driven Architecture**: Robust outbox pattern implementation in order-service
- **Resilience Patterns**: Complete Resilience4j implementation with circuit breakers, retries, and timeouts
- **Security Foundation**: OAuth2/JWT authentication with Keycloak integration
- **API Gateway**: Functional routing and load balancing

### ❌ Areas for Improvement
- **Service Discovery**: Using Eureka in Kubernetes (redundant)
- **Configuration Management**: Hardcoded URLs and environment-specific configs
- **Health Checks**: No Kubernetes-native health checks
- **Resource Management**: No resource limits or requests
- **Security**: Missing network policies and RBAC
- **Monitoring**: Basic observability setup

## Recommended Changes

### 1. Service Discovery Optimization

**Current Issue**: Using Eureka in Kubernetes environment
**Solution**: Implement hybrid service discovery with Kubernetes-native fallback

#### 1.1 Remove Eureka Dependency (Optional)
```yaml
# Option A: Complete removal
# Remove from all application.yml files
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

# Option B: Conditional usage (Recommended)
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

# application-local.yml
eureka:
  client:
    enabled: true
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761}/eureka/

# application-k8s.yml
eureka:
  client:
    enabled: false
```

#### 1.2 Update Service URLs
```java
// Current implementation
@Value("${payment.service.url:http://payment-service:8085}")
private String paymentServiceUrl;

// Recommended implementation
@Value("${payment.service.url:http://payment-service}")
private String paymentServiceUrl;
```

### 2. Configuration Management

#### 2.1 Environment-Specific ConfigMaps
```yaml
# k8s/config/local-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mambogo-local-config
  namespace: mambogo-local
data:
  payment-service-url: "http://payment-service:8085"
  product-service-url: "http://product-service:8082"
  cart-service-url: "http://cart-service:8083"
  kafka-bootstrap-servers: "kafka-service:9092"
  eureka-server-url: "http://eureka-server:8761"
  database-url: "jdbc:postgresql://postgres:5432/orders"
  database-username: "postgres"
  database-password: "postgres"

---
# k8s/config/demo-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mambogo-demo-config
  namespace: mambogo-demo
data:
  payment-service-url: "http://payment-service"
  product-service-url: "http://product-service"
  cart-service-url: "http://cart-service"
  kafka-bootstrap-servers: "kafka-service:9092"
  database-url: "jdbc:postgresql://orders-db:5432/orders"

---
# k8s/config/prod-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mambogo-prod-config
  namespace: mambogo-prod
data:
  payment-service-url: "http://payment-service"
  product-service-url: "http://product-service"
  cart-service-url: "http://cart-service"
  kafka-bootstrap-servers: "kafka-service:9092"
  database-url: "jdbc:postgresql://orders-db:5432/orders"
```

#### 2.2 Database Secrets
```yaml
# k8s/secrets/database-secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: database-credentials
  namespace: mambogo-prod
type: Opaque
data:
  username: cG9zdGdyZXM=  # base64 encoded
  password: cG9zdGdyZXM=  # base64 encoded
```

#### 2.3 Updated Application Configuration
```yaml
# application.yml (base configuration)
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5433/orders}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}

# Service URLs
payment:
  service:
    url: ${PAYMENT_SERVICE_URL:http://payment-service}
product:
  service:
    url: ${PRODUCT_SERVICE_URL:http://product-service}
cart:
  service:
    url: ${CART_SERVICE_URL:http://cart-service}
```

### 3. Kubernetes Services

#### 3.1 Core Application Services
```yaml
# k8s/services/order-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
  labels:
    app: order-service
spec:
  selector:
    app: order-service
  ports:
    - protocol: TCP
      port: 8084
      targetPort: 8084
  type: ClusterIP

---
# k8s/services/payment-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: payment-service
  labels:
    app: payment-service
spec:
  selector:
    app: payment-service
  ports:
    - protocol: TCP
      port: 8085
      targetPort: 8085
  type: ClusterIP

---
# k8s/services/product-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: product-service
  labels:
    app: product-service
spec:
  selector:
    app: product-service
  ports:
    - protocol: TCP
      port: 8082
      targetPort: 8082
  type: ClusterIP

---
# k8s/services/cart-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: cart-service
  labels:
    app: cart-service
spec:
  selector:
    app: cart-service
  ports:
    - protocol: TCP
      port: 8083
      targetPort: 8083
  type: ClusterIP

---
# k8s/services/gateway-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: gateway-service
  labels:
    app: gateway-service
spec:
  selector:
    app: gateway-service
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: LoadBalancer  # For external access
```

#### 3.2 Infrastructure Services
```yaml
# k8s/services/infrastructure.yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka-service
  labels:
    app: kafka
spec:
  selector:
    app: kafka
  ports:
    - protocol: TCP
      port: 9092
      targetPort: 9092
  type: ClusterIP

---
apiVersion: v1
kind: Service
metadata:
  name: orders-db
  labels:
    app: postgres
    database: orders
spec:
  selector:
    app: postgres
    database: orders
  ports:
    - protocol: TCP
      port: 5432
      targetPort: 5432
  type: ClusterIP

---
apiVersion: v1
kind: Service
metadata:
  name: eureka-server
  labels:
    app: eureka-server
spec:
  selector:
    app: eureka-server
  ports:
    - protocol: TCP
      port: 8761
      targetPort: 8761
  type: ClusterIP
```

### 4. Deployments with Health Checks

#### 4.1 Order Service Deployment
```yaml
# k8s/deployments/order-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  labels:
    app: order-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: mambogo/order-service:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8084
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: PAYMENT_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: mambogo-config
              key: payment-service-url
        - name: PRODUCT_SERVICE_URL
          valueFrom:
            configMapKeyRef:
              name: mambogo-config
              key: product-service-url
        - name: KAFKA_BOOTSTRAP_SERVERS
          valueFrom:
            configMapKeyRef:
              name: mambogo-config
              key: kafka-bootstrap-servers
        - name: DB_URL
          valueFrom:
            configMapKeyRef:
              name: mambogo-config
              key: database-url
        - name: DB_USER
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: username
        - name: DB_PASS
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
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
            port: 8084
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
      volumes:
      - name: config-volume
        configMap:
          name: mambogo-config
```

#### 4.2 Other Service Deployments
Similar deployment configurations for:
- `k8s/deployments/payment-service.yaml`
- `k8s/deployments/product-service.yaml`
- `k8s/deployments/cart-service.yaml`
- `k8s/deployments/gateway-service.yaml`

### 5. Security Enhancements

#### 5.1 Network Policies
```yaml
# k8s/security/network-policies.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: order-service-network-policy
spec:
  podSelector:
    matchLabels:
      app: order-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: gateway-service
    ports:
    - protocol: TCP
      port: 8084
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: payment-service
    ports:
    - protocol: TCP
      port: 8085
  - to:
    - podSelector:
        matchLabels:
          app: product-service
    ports:
    - protocol: TCP
      port: 8082
  - to:
    - podSelector:
        matchLabels:
          app: kafka
    ports:
    - protocol: TCP
      port: 9092
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
```

#### 5.2 RBAC Configuration
```yaml
# k8s/security/rbac.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: mambogo-service-account
  namespace: mambogo-prod

---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: mambogo-role
  namespace: mambogo-prod
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods", "services"]
  verbs: ["get", "list", "watch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: mambogo-role-binding
  namespace: mambogo-prod
subjects:
- kind: ServiceAccount
  name: mambogo-service-account
  namespace: mambogo-prod
roleRef:
  kind: Role
  name: mambogo-role
  apiGroup: rbac.authorization.k8s.io
```

### 6. Monitoring and Observability

#### 6.1 Prometheus ServiceMonitor
```yaml
# k8s/monitoring/prometheus.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: order-service-monitor
  namespace: mambogo-prod
spec:
  selector:
    matchLabels:
      app: order-service
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

#### 6.2 Grafana Dashboards
```yaml
# k8s/monitoring/grafana-dashboards.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards
  namespace: monitoring
data:
  mambogo-dashboard.json: |
    {
      "dashboard": {
        "title": "Mambogo E-commerce Dashboard",
        "panels": [
          {
            "title": "Order Service Metrics",
            "type": "graph",
            "targets": [
              {
                "expr": "rate(http_requests_total{service=\"order-service\"}[5m])",
                "legendFormat": "{{method}} {{endpoint}}"
              }
            ]
          }
        ]
      }
    }
```

### 7. Multi-Environment Setup

#### 7.1 Local Development (Docker Compose)
```yaml
# infra/docker-compose.local.yml
version: '3.8'
services:
  # Infrastructure
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: orders
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
      - "29092:29092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  eureka-server:
    image: mambogo/eureka-server:latest
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=local

  # Application Services
  order-service:
    image: mambogo/order-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - DB_URL=jdbc:postgresql://postgres:5432/orders
      - KAFKA_BOOTSTRAP_SERVERS=kafka:29092
      - EUREKA_SERVER_URL=http://eureka-server:8761
      - PAYMENT_SERVICE_URL=http://payment-service:8085
      - PRODUCT_SERVICE_URL=http://product-service:8082
    ports:
      - "8084:8084"
    depends_on:
      - postgres
      - kafka
      - eureka-server

  payment-service:
    image: mambogo/payment-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - EUREKA_SERVER_URL=http://eureka-server:8761
    ports:
      - "8085:8085"
    depends_on:
      - eureka-server

  product-service:
    image: mambogo/product-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - EUREKA_SERVER_URL=http://eureka-server:8761
    ports:
      - "8082:8082"
    depends_on:
      - eureka-server

  cart-service:
    image: mambogo/cart-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - EUREKA_SERVER_URL=http://eureka-server:8761
    ports:
      - "8083:8083"
    depends_on:
      - eureka-server

  gateway-service:
    image: mambogo/gateway-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - EUREKA_SERVER_URL=http://eureka-server:8761
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server

volumes:
  postgres_data:
```

#### 7.2 Testing Environment
```yaml
# infra/docker-compose.test.yml
version: '3.8'
services:
  test-db:
    image: postgres:15
    environment:
      POSTGRES_DB: test_orders
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5434:5432"

  test-kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: test-zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://test-kafka:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - test-zookeeper

  test-zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  order-service-test:
    image: mambogo/order-service:test
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - DB_URL=jdbc:postgresql://test-db:5432/test_orders
      - KAFKA_BOOTSTRAP_SERVERS=test-kafka:29092
    ports:
      - "8086:8084"
```

### 8. Development Scripts

#### 8.1 Local Development Setup
```bash
#!/bin/bash
# scripts/dev-setup.sh

echo "Setting up local development environment..."

# Build all services
echo "Building services..."
cd backend
mvn clean package -DskipTests

# Start infrastructure
echo "Starting infrastructure..."
cd ../infra
docker-compose -f docker-compose.local.yml up -d postgres kafka zookeeper eureka-server

# Wait for infrastructure to be ready
echo "Waiting for infrastructure to be ready..."
sleep 30

# Start application services
echo "Starting application services..."
docker-compose -f docker-compose.local.yml up -d order-service payment-service product-service cart-service gateway-service

echo "Local development environment is ready!"
echo "Gateway: http://localhost:8080"
echo "Eureka: http://localhost:8761"
echo "Order Service: http://localhost:8084"
echo "Payment Service: http://localhost:8085"
echo "Product Service: http://localhost:8082"
echo "Cart Service: http://localhost:8083"
```

#### 8.2 Windows Development Setup
```batch
@echo off
REM scripts/dev-setup.bat

echo Setting up local development environment...

REM Build all services
echo Building services...
cd backend
call mvn clean package -DskipTests

REM Start infrastructure
echo Starting infrastructure...
cd ..\infra
docker-compose -f docker-compose.local.yml up -d postgres kafka zookeeper eureka-server

REM Wait for infrastructure to be ready
echo Waiting for infrastructure to be ready...
timeout /t 30 /nobreak

REM Start application services
echo Starting application services...
docker-compose -f docker-compose.local.yml up -d order-service payment-service product-service cart-service gateway-service

echo Local development environment is ready!
echo Gateway: http://localhost:8080
echo Eureka: http://localhost:8761
echo Order Service: http://localhost:8084
echo Payment Service: http://localhost:8085
echo Product Service: http://localhost:8082
echo Cart Service: http://localhost:8083
```

#### 8.3 Kubernetes Deployment Scripts
```bash
#!/bin/bash
# scripts/k8s-deploy.sh

ENVIRONMENT=$1
NAMESPACE="mambogo-${ENVIRONMENT}"

if [ -z "$ENVIRONMENT" ]; then
    echo "Usage: $0 <environment>"
    echo "Environments: local, demo, prod"
    exit 1
fi

echo "Deploying to ${ENVIRONMENT} environment..."

# Create namespace
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# Apply configurations
kubectl apply -f k8s/config/${ENVIRONMENT}-config.yaml -n ${NAMESPACE}
kubectl apply -f k8s/secrets/database-secrets.yaml -n ${NAMESPACE}

# Deploy infrastructure
kubectl apply -f k8s/services/infrastructure.yaml -n ${NAMESPACE}
kubectl apply -f k8s/deployments/infrastructure.yaml -n ${NAMESPACE}

# Deploy application services
kubectl apply -f k8s/services/ -n ${NAMESPACE}
kubectl apply -f k8s/deployments/ -n ${NAMESPACE}

# Apply security policies
kubectl apply -f k8s/security/ -n ${NAMESPACE}

# Apply monitoring
kubectl apply -f k8s/monitoring/ -n ${NAMESPACE}

echo "Deployment to ${ENVIRONMENT} completed!"
echo "Access the application: kubectl port-forward svc/gateway-service 8080:8080 -n ${NAMESPACE}"
```

### 9. Testing Strategy

#### 9.1 Unit Tests
```java
// backend/order-service/src/test/java/com/mambogo/order/client/PaymentClientTest.java
@ExtendWith(MockitoExtension.class)
class PaymentClientTest {
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @InjectMocks
    private PaymentClient paymentClient;
    
    @Test
    void testProcessPaymentSuccess() {
        // Test implementation
    }
    
    @Test
    void testProcessPaymentFallback() {
        // Test fallback behavior
    }
}
```

#### 9.2 Integration Tests
```java
// backend/order-service/src/test/java/com/mambogo/order/integration/ResilienceIntegrationTest.java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "payment.service.url=http://localhost:8085",
    "product.service.url=http://localhost:8082"
})
class ResilienceIntegrationTest {
    
    @Test
    void testCircuitBreakerBehavior() {
        // Test circuit breaker functionality
    }
    
    @Test
    void testRetryMechanism() {
        // Test retry behavior
    }
}
```

#### 9.3 End-to-End Tests
```yaml
# k8s/tests/e2e-tests.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: e2e-tests
spec:
  template:
    spec:
      containers:
      - name: e2e-tests
        image: mambogo/e2e-tests:latest
        env:
        - name: GATEWAY_URL
          value: "http://gateway-service:8080"
        - name: TEST_ENVIRONMENT
          value: "k8s"
      restartPolicy: Never
  backoffLimit: 3
```

### 10. Deployment Strategy

#### 10.1 Blue-Green Deployment
```yaml
# k8s/deployments/blue-green.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: order-service-rollout
spec:
  replicas: 3
  strategy:
    blueGreen:
      activeService: order-service-active
      previewService: order-service-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: order-service-active
      postPromotionAnalysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: order-service-active
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: mambogo/order-service:latest
```

#### 10.2 Canary Deployment
```yaml
# k8s/deployments/canary.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: order-service-canary
spec:
  replicas: 5
  strategy:
    canary:
      steps:
      - setWeight: 20
      - pause: {duration: 60s}
      - setWeight: 40
      - pause: {duration: 60s}
      - setWeight: 60
      - pause: {duration: 60s}
      - setWeight: 80
      - pause: {duration: 60s}
      - setWeight: 100
      analysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: order-service
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: mambogo/order-service:latest
```

### 11. Rollback Plan

#### 11.1 Automatic Rollback
```yaml
# k8s/deployments/rollback-policy.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  revisionHistoryLimit: 10  # Keep 10 previous revisions
  template:
    spec:
      containers:
      - name: order-service
        image: mambogo/order-service:latest
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8084
          initialDelaySeconds: 60
          periodSeconds: 30
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 3
```

#### 11.2 Manual Rollback Script
```bash
#!/bin/bash
# scripts/rollback.sh

SERVICE_NAME=$1
REVISION=$2
NAMESPACE=${3:-mambogo-prod}

if [ -z "$SERVICE_NAME" ] || [ -z "$REVISION" ]; then
    echo "Usage: $0 <service-name> <revision> [namespace]"
    echo "Example: $0 order-service 2 mambogo-prod"
    exit 1
fi

echo "Rolling back ${SERVICE_NAME} to revision ${REVISION} in namespace ${NAMESPACE}..."

# Rollback to specific revision
kubectl rollout undo deployment/${SERVICE_NAME} --to-revision=${REVISION} -n ${NAMESPACE}

# Wait for rollback to complete
kubectl rollout status deployment/${SERVICE_NAME} -n ${NAMESPACE}

echo "Rollback completed!"
```

## Implementation Phases

### Phase 1: Foundation (Week 1-2)
- [ ] Create Kubernetes namespace structure
- [ ] Implement ConfigMaps and Secrets
- [ ] Update application configuration for multi-environment support
- [ ] Create basic Kubernetes services and deployments
- [ ] Add health checks and resource limits

### Phase 2: Security & Networking (Week 3)
- [ ] Implement Network Policies
- [ ] Configure RBAC
- [ ] Set up service mesh (optional)
- [ ] Implement security best practices

### Phase 3: Monitoring & Observability (Week 4)
- [ ] Deploy Prometheus and Grafana
- [ ] Configure ServiceMonitors
- [ ] Set up distributed tracing
- [ ] Create monitoring dashboards

### Phase 4: Testing & Validation (Week 5)
- [ ] Implement comprehensive test suites
- [ ] Set up CI/CD pipelines
- [ ] Create deployment strategies
- [ ] Validate all environments

### Phase 5: Documentation & Training (Week 6)
- [ ] Update documentation
- [ ] Create runbooks
- [ ] Train operations team
- [ ] Final validation and go-live

## Success Metrics

### Performance Metrics
- **Response Time**: < 200ms for 95th percentile
- **Availability**: 99.9% uptime
- **Error Rate**: < 0.1% for critical paths

### Operational Metrics
- **Deployment Time**: < 5 minutes for full deployment
- **Rollback Time**: < 2 minutes
- **MTTR**: < 15 minutes for critical issues

### Security Metrics
- **Vulnerability Scan**: 0 critical vulnerabilities
- **Compliance**: 100% compliance with security policies
- **Access Control**: Proper RBAC implementation

## Risk Mitigation

### Technical Risks
- **Service Discovery Issues**: Implement fallback mechanisms
- **Configuration Drift**: Use GitOps practices
- **Performance Degradation**: Implement proper monitoring and alerting

### Operational Risks
- **Deployment Failures**: Implement comprehensive testing
- **Data Loss**: Regular backups and disaster recovery procedures
- **Security Breaches**: Regular security audits and penetration testing

## Conclusion

This comprehensive plan provides a roadmap for transforming the Mambogo e-commerce platform into a Kubernetes-native, production-ready system while maintaining seamless local development capabilities. The phased approach ensures minimal disruption while delivering maximum value at each stage.

The implementation focuses on:
- **Reliability**: Robust health checks, monitoring, and rollback capabilities
- **Security**: Network policies, RBAC, and security best practices
- **Scalability**: Kubernetes-native service discovery and load balancing
- **Maintainability**: Clear configuration management and documentation
- **Developer Experience**: Seamless local development and testing environments

By following this plan, the team will achieve a modern, cloud-native architecture that can scale efficiently and operate reliably in production environments.
