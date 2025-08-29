#!/bin/bash

# Mambogo Kubernetes Deployment Script
# Usage: ./k8s-deploy.sh <environment> [namespace]
# Environments: local, demo, prod

set -e

ENVIRONMENT=$1
NAMESPACE=${2:-"mambogo-${ENVIRONMENT}"}

if [ -z "$ENVIRONMENT" ]; then
    echo "Usage: $0 <environment> [namespace]"
    echo "Environments: local, demo, prod"
    echo "Example: $0 prod"
    echo "Example: $0 demo mambogo-demo"
    exit 1
fi

echo "🚀 Deploying Mambogo to ${ENVIRONMENT} environment..."
echo "📦 Namespace: ${NAMESPACE}"

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(local|demo|prod)$ ]]; then
    echo "❌ Invalid environment: $ENVIRONMENT"
    echo "Valid environments: local, demo, prod"
    exit 1
fi

# Create namespace
echo "📋 Creating namespace..."
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# Apply configurations
echo "⚙️  Applying configurations..."
kubectl apply -f k8s/config/${ENVIRONMENT}-config.yaml -n ${NAMESPACE}
kubectl apply -f k8s/secrets/database-secrets.yaml -n ${NAMESPACE}

# Apply RBAC
echo "🔐 Applying RBAC..."
kubectl apply -f k8s/security/rbac.yaml -n ${NAMESPACE}

# Deploy infrastructure services
echo "🏗️  Deploying infrastructure services..."
kubectl apply -f k8s/services/infrastructure.yaml -n ${NAMESPACE}

# Deploy application services
echo "🚀 Deploying application services..."
kubectl apply -f k8s/services/order-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/services/payment-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/services/product-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/services/cart-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/services/gateway-service.yaml -n ${NAMESPACE}

# Deploy application deployments
echo "📦 Deploying application deployments..."
kubectl apply -f k8s/deployments/order-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/deployments/payment-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/deployments/product-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/deployments/cart-service.yaml -n ${NAMESPACE}
kubectl apply -f k8s/deployments/gateway-service.yaml -n ${NAMESPACE}

# Apply security policies (only for production)
if [ "$ENVIRONMENT" = "prod" ]; then
    echo "🔒 Applying security policies..."
    kubectl apply -f k8s/security/network-policies.yaml -n ${NAMESPACE}
fi

# Apply monitoring (only for production)
if [ "$ENVIRONMENT" = "prod" ]; then
    echo "📊 Applying monitoring..."
    kubectl apply -f k8s/monitoring/prometheus.yaml -n ${NAMESPACE}
    kubectl apply -f k8s/monitoring/grafana-dashboards.yaml -n monitoring
fi

# Wait for deployments to be ready
echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/order-service -n ${NAMESPACE}
kubectl wait --for=condition=available --timeout=300s deployment/payment-service -n ${NAMESPACE}
kubectl wait --for=condition=available --timeout=300s deployment/product-service -n ${NAMESPACE}
kubectl wait --for=condition=available --timeout=300s deployment/cart-service -n ${NAMESPACE}
kubectl wait --for=condition=available --timeout=300s deployment/gateway-service -n ${NAMESPACE}

echo "✅ Deployment to ${ENVIRONMENT} completed successfully!"
echo ""
echo "🌐 Access Information:"
echo "   Gateway Service: kubectl port-forward svc/gateway-service 8080:8080 -n ${NAMESPACE}"
echo "   Eureka Server: kubectl port-forward svc/eureka-server 8761:8761 -n ${NAMESPACE}"
echo ""
echo "📊 Monitoring:"
echo "   Prometheus: kubectl port-forward svc/prometheus 9090:9090 -n monitoring"
echo "   Grafana: kubectl port-forward svc/grafana 3000:3000 -n monitoring"
echo ""
echo "🔍 Check deployment status:"
echo "   kubectl get pods -n ${NAMESPACE}"
echo "   kubectl get services -n ${NAMESPACE}"
