#!/bin/bash

# Eureka Service Discovery Verification Script
# This script verifies that all services are properly registered with Eureka

echo "=== Eureka Service Discovery Verification ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a service is running
check_service() {
    local service_name=$1
    local port=$2
    local url=$3
    
    echo -n "Checking $service_name (port $port)... "
    
    if curl -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Running${NC}"
        return 0
    else
        echo -e "${RED}✗ Not running${NC}"
        return 1
    fi
}

# Function to check Eureka registration
check_eureka_registration() {
    local service_name=$1
    
    echo -n "Checking Eureka registration for $service_name... "
    
    # Check if service is registered in Eureka
    if curl -s "http://localhost:8761/eureka/apps/$service_name" | grep -q "UP"; then
        echo -e "${GREEN}✓ Registered and UP${NC}"
        return 0
    else
        echo -e "${RED}✗ Not registered or DOWN${NC}"
        return 1
    fi
}

# Function to test gateway routing
test_gateway_routing() {
    local service_path=$1
    local service_name=$2
    
    echo -n "Testing gateway routing to $service_name... "
    
    if curl -s "http://localhost:8080$service_path/actuator/health" | grep -q "UP"; then
        echo -e "${GREEN}✓ Routing working${NC}"
        return 0
    else
        echo -e "${RED}✗ Routing failed${NC}"
        return 1
    fi
}

echo "1. Checking Eureka Server..."
check_service "Eureka Server" 8761 "http://localhost:8761/actuator/health"

echo ""
echo "2. Checking individual services..."

# Check each service directly
check_service "Gateway Service" 8080 "http://localhost:8080/actuator/health"
check_service "Product Service" 8082 "http://localhost:8082/actuator/health"
check_service "Cart Service" 8083 "http://localhost:8083/actuator/health"
check_service "Order Service" 8084 "http://localhost:8084/actuator/health"
check_service "Payment Service" 8085 "http://localhost:8085/actuator/health"

echo ""
echo "3. Checking Eureka registrations..."

# Check Eureka registrations
check_eureka_registration "GATEWAY-SERVICE"
check_eureka_registration "PRODUCT-SERVICE"
check_eureka_registration "CART-SERVICE"
check_eureka_registration "ORDER-SERVICE"
check_eureka_registration "PAYMENT-SERVICE"

echo ""
echo "4. Testing Gateway routing..."

# Test gateway routing to each service
test_gateway_routing "/api/products" "Product Service"
test_gateway_routing "/api/cart" "Cart Service"
test_gateway_routing "/api/orders" "Order Service"
test_gateway_routing "/api/payments" "Payment Service"

echo ""
echo "5. Eureka Dashboard URLs:"
echo -e "${YELLOW}Eureka Dashboard:${NC} http://localhost:8761"
echo -e "${YELLOW}Gateway Health:${NC} http://localhost:8080/actuator/health"
echo -e "${YELLOW}Product Service:${NC} http://localhost:8080/api/products/actuator/health"
echo -e "${YELLOW}Cart Service:${NC} http://localhost:8080/api/cart/actuator/health"
echo -e "${YELLOW}Order Service:${NC} http://localhost:8080/api/orders/actuator/health"
echo -e "${YELLOW}Payment Service:${NC} http://localhost:8080/api/payments/actuator/health"

echo ""
echo "=== Verification Complete ==="
echo ""
echo "If all services show ✓, your Eureka setup is working correctly!"
echo "If any service shows ✗, check the service logs and ensure it's running."
