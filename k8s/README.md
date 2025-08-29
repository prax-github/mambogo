# Mambogo Kubernetes Implementation

This directory contains the complete Kubernetes implementation for the Mambogo e-commerce platform, following the optimization plan outlined in `devplan/KUBERNETES_OPTIMIZATION_PLAN.md`.

## 📁 Directory Structure

```
k8s/
├── config/                    # Environment-specific ConfigMaps
│   ├── local-config.yaml     # Local development configuration
│   ├── demo-config.yaml      # Demo environment configuration
│   └── prod-config.yaml      # Production environment configuration
├── secrets/                   # Kubernetes Secrets
│   └── database-secrets.yaml # Database credentials
├── services/                  # Kubernetes Services
│   ├── order-service.yaml    # Order service
│   ├── payment-service.yaml  # Payment service
│   ├── product-service.yaml  # Product service
│   ├── cart-service.yaml     # Cart service
│   ├── gateway-service.yaml  # Gateway service
│   └── infrastructure.yaml   # Infrastructure services
├── deployments/              # Kubernetes Deployments
│   ├── order-service.yaml    # Order service deployment
│   ├── payment-service.yaml  # Payment service deployment
│   ├── product-service.yaml  # Product service deployment
│   ├── cart-service.yaml     # Cart service deployment
│   └── gateway-service.yaml  # Gateway service deployment
├── security/                 # Security configurations
│   ├── rbac.yaml            # RBAC (ServiceAccounts, Roles, RoleBindings)
│   └── network-policies.yaml # Network policies
├── monitoring/               # Monitoring configurations
│   ├── prometheus.yaml      # Prometheus ServiceMonitors
│   └── grafana-dashboards.yaml # Grafana dashboards
└── README.md                # This file
```

## 🚀 Quick Start

### Prerequisites

- Kubernetes cluster (local: minikube, kind, or cloud: GKE, EKS, AKS)
- kubectl configured
- Docker images built and pushed to registry

### Deployment

#### Using Scripts (Recommended)

**Linux/Mac:**
```bash
# Deploy to local environment
./scripts/k8s-deploy.sh local

# Deploy to demo environment
./scripts/k8s-deploy.sh demo

# Deploy to production environment
./scripts/k8s-deploy.sh prod
```

**Windows:**
```batch
# Deploy to local environment
scripts\k8s-deploy.bat local

# Deploy to demo environment
scripts\k8s-deploy.bat demo

# Deploy to production environment
scripts\k8s-deploy.bat prod
```

#### Manual Deployment

```bash
# 1. Create namespace
kubectl create namespace mambogo-prod

# 2. Apply configurations
kubectl apply -f k8s/config/prod-config.yaml -n mambogo-prod
kubectl apply -f k8s/secrets/database-secrets.yaml -n mambogo-prod

# 3. Apply RBAC
kubectl apply -f k8s/security/rbac.yaml -n mambogo-prod

# 4. Deploy services
kubectl apply -f k8s/services/ -n mambogo-prod

# 5. Deploy applications
kubectl apply -f k8s/deployments/ -n mambogo-prod

# 6. Apply security policies (production only)
kubectl apply -f k8s/security/network-policies.yaml -n mambogo-prod

# 7. Apply monitoring (production only)
kubectl apply -f k8s/monitoring/ -n mambogo-prod
```

## 🔧 Configuration

### Environment-Specific Configurations

Each environment has its own ConfigMap with appropriate settings:

- **Local**: Uses port-specific URLs and local development settings
- **Demo**: Uses Kubernetes-native service URLs
- **Production**: Uses production-ready settings with security policies

### Service Discovery

- **Local/Demo**: Eureka enabled for service discovery
- **Production**: Eureka disabled, using Kubernetes native service discovery

### Service URLs

```yaml
# Local Environment
payment-service-url: "http://payment-service:8085"

# Demo/Production Environment
payment-service-url: "http://payment-service"
```

## 🔒 Security

### RBAC (Role-Based Access Control)

- Service accounts for each namespace
- Minimal required permissions
- Namespace-scoped roles

### Network Policies

- **Production Only**: Strict network policies
- Service-to-service communication control
- Database access restrictions

### Secrets Management

- Database credentials stored as Kubernetes secrets
- Base64 encoded (consider using external secret management for production)

## 📊 Monitoring

### Prometheus Integration

- ServiceMonitors for all services
- Metrics exposed via `/actuator/prometheus`
- 30-second scrape intervals

### Grafana Dashboards

- **Mambogo Overview Dashboard**: System metrics, response times, error rates
- **Business Metrics Dashboard**: Orders, payments, success rates

### Health Checks

- **Liveness Probe**: `/actuator/health`
- **Readiness Probe**: `/actuator/health/readiness`
- **Startup Probe**: Automatic health check after startup

## 🔄 Rollback

### Using Rollback Script

```bash
# Rollback order service to revision 2
./scripts/rollback.sh order-service 2 mambogo-prod

# Rollback payment service to revision 1
./scripts/rollback.sh payment-service 1
```

### Manual Rollback

```bash
# Check rollout history
kubectl rollout history deployment/order-service -n mambogo-prod

# Rollback to specific revision
kubectl rollout undo deployment/order-service --to-revision=2 -n mambogo-prod

# Check rollback status
kubectl rollout status deployment/order-service -n mambogo-prod
```

## 🌐 Access

### Port Forwarding

```bash
# Gateway Service
kubectl port-forward svc/gateway-service 8080:8080 -n mambogo-prod

# Eureka Server
kubectl port-forward svc/eureka-server 8761:8761 -n mambogo-prod

# Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n monitoring

# Grafana
kubectl port-forward svc/grafana 3000:3000 -n monitoring
```

### Load Balancer (Production)

```bash
# Get external IP
kubectl get svc gateway-service -n mambogo-prod

# Access via external IP
curl http://<EXTERNAL-IP>:8080/actuator/health
```

## 📈 Scaling

### Horizontal Pod Autoscaling

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
```

### Manual Scaling

```bash
# Scale order service to 5 replicas
kubectl scale deployment order-service --replicas=5 -n mambogo-prod

# Check scaling status
kubectl get pods -l app=order-service -n mambogo-prod
```

## 🔍 Troubleshooting

### Common Issues

1. **Pods not starting**
   ```bash
   kubectl describe pod <pod-name> -n mambogo-prod
   kubectl logs <pod-name> -n mambogo-prod
   ```

2. **Services not accessible**
   ```bash
   kubectl get svc -n mambogo-prod
   kubectl describe svc <service-name> -n mambogo-prod
   ```

3. **Configuration issues**
   ```bash
   kubectl get configmap mambogo-prod-config -n mambogo-prod -o yaml
   kubectl get secret database-credentials -n mambogo-prod -o yaml
   ```

### Health Checks

```bash
# Check all pods
kubectl get pods -n mambogo-prod

# Check service endpoints
kubectl get endpoints -n mambogo-prod

# Check events
kubectl get events -n mambogo-prod --sort-by='.lastTimestamp'
```

## 📋 Best Practices

### Resource Management

- **Requests**: Minimum resources required
- **Limits**: Maximum resources allowed
- **Health Checks**: Proper liveness and readiness probes

### Security

- **Network Policies**: Restrict unnecessary traffic
- **RBAC**: Principle of least privilege
- **Secrets**: Use external secret management in production

### Monitoring

- **Metrics**: Comprehensive application metrics
- **Logging**: Structured logging with correlation IDs
- **Tracing**: Distributed tracing with Zipkin

### Deployment

- **Rolling Updates**: Zero-downtime deployments
- **Rollback Strategy**: Quick rollback capabilities
- **Environment Separation**: Clear environment boundaries

## 🔗 Related Documentation

- [Kubernetes Optimization Plan](../devplan/KUBERNETES_OPTIMIZATION_PLAN.md)
- [Resilience4j Implementation](../docs/resilience.md)
- [Service Discovery Setup](../EUREKA_SERVICE_DISCOVERY_SETUP.md)
- [Config Server Setup](../CONFIG_SERVER_SETUP.md)
