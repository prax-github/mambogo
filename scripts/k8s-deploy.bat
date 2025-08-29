@echo off
REM Mambogo Kubernetes Deployment Script for Windows
REM Usage: k8s-deploy.bat <environment> [namespace]
REM Environments: local, demo, prod

setlocal enabledelayedexpansion

set ENVIRONMENT=%1
set NAMESPACE=%2

if "%NAMESPACE%"=="" set NAMESPACE=mambogo-%ENVIRONMENT%

if "%ENVIRONMENT%"=="" (
    echo Usage: %0 ^<environment^> [namespace]
    echo Environments: local, demo, prod
    echo Example: %0 prod
    echo Example: %0 demo mambogo-demo
    exit /b 1
)

echo 🚀 Deploying Mambogo to %ENVIRONMENT% environment...
echo 📦 Namespace: %NAMESPACE%

REM Validate environment
if not "%ENVIRONMENT%"=="local" if not "%ENVIRONMENT%"=="demo" if not "%ENVIRONMENT%"=="prod" (
    echo ❌ Invalid environment: %ENVIRONMENT%
    echo Valid environments: local, demo, prod
    exit /b 1
)

REM Create namespace
echo 📋 Creating namespace...
kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -

REM Apply configurations
echo ⚙️  Applying configurations...
kubectl apply -f k8s/config/%ENVIRONMENT%-config.yaml -n %NAMESPACE%
kubectl apply -f k8s/secrets/database-secrets.yaml -n %NAMESPACE%

REM Apply RBAC
echo 🔐 Applying RBAC...
kubectl apply -f k8s/security/rbac.yaml -n %NAMESPACE%

REM Deploy infrastructure services
echo 🏗️  Deploying infrastructure services...
kubectl apply -f k8s/services/infrastructure.yaml -n %NAMESPACE%

REM Deploy application services
echo 🚀 Deploying application services...
kubectl apply -f k8s/services/order-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/services/payment-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/services/product-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/services/cart-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/services/gateway-service.yaml -n %NAMESPACE%

REM Deploy application deployments
echo 📦 Deploying application deployments...
kubectl apply -f k8s/deployments/order-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/deployments/payment-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/deployments/product-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/deployments/cart-service.yaml -n %NAMESPACE%
kubectl apply -f k8s/deployments/gateway-service.yaml -n %NAMESPACE%

REM Apply security policies (only for production)
if "%ENVIRONMENT%"=="prod" (
    echo 🔒 Applying security policies...
    kubectl apply -f k8s/security/network-policies.yaml -n %NAMESPACE%
)

REM Apply monitoring (only for production)
if "%ENVIRONMENT%"=="prod" (
    echo 📊 Applying monitoring...
    kubectl apply -f k8s/monitoring/prometheus.yaml -n %NAMESPACE%
    kubectl apply -f k8s/monitoring/grafana-dashboards.yaml -n monitoring
)

REM Wait for deployments to be ready
echo ⏳ Waiting for deployments to be ready...
kubectl wait --for=condition=available --timeout=300s deployment/order-service -n %NAMESPACE%
kubectl wait --for=condition=available --timeout=300s deployment/payment-service -n %NAMESPACE%
kubectl wait --for=condition=available --timeout=300s deployment/product-service -n %NAMESPACE%
kubectl wait --for=condition=available --timeout=300s deployment/cart-service -n %NAMESPACE%
kubectl wait --for=condition=available --timeout=300s deployment/gateway-service -n %NAMESPACE%

echo ✅ Deployment to %ENVIRONMENT% completed successfully!
echo.
echo 🌐 Access Information:
echo    Gateway Service: kubectl port-forward svc/gateway-service 8080:8080 -n %NAMESPACE%
echo    Eureka Server: kubectl port-forward svc/eureka-server 8761:8761 -n %NAMESPACE%
echo.
echo 📊 Monitoring:
echo    Prometheus: kubectl port-forward svc/prometheus 9090:9090 -n monitoring
echo    Grafana: kubectl port-forward svc/grafana 3000:3000 -n monitoring
echo.
echo 🔍 Check deployment status:
echo    kubectl get pods -n %NAMESPACE%
echo    kubectl get services -n %NAMESPACE%
