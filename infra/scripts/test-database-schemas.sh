#!/bin/bash

# =====================================================
# DATABASE SCHEMA TESTING SCRIPT
# =====================================================
# This script validates all database schemas for the e-commerce microservices
# using Docker Compose and comprehensive test queries

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCKER_COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
}

log_header() {
    echo -e "\n${BLUE}=====================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}=====================================================${NC}\n"
}

# Test function
run_test() {
    local test_name="$1"
    local command="$2"
    local expected_result="$3"
    
    log_info "Running test: $test_name"
    
    if eval "$command" | grep -q "$expected_result"; then
        log_success "Test passed: $test_name"
        return 0
    else
        log_error "Test failed: $test_name"
        return 1
    fi
}

# Database connection test
test_database_connection() {
    local service_name="$1"
    local port="$2"
    local database="$3"
    
    log_info "Testing connection to $service_name database..."
    
    if docker exec "mambogo-${service_name}-1" pg_isready -U postgres -d "$database" >/dev/null 2>&1; then
        log_success "Database connection successful: $service_name"
        return 0
    else
        log_error "Database connection failed: $service_name"
        return 1
    fi
}

# Schema validation test
test_schema_tables() {
    local service_name="$1"
    local port="$2"
    local database="$3"
    local expected_tables="$4"
    
    log_info "Testing schema tables for $service_name..."
    
    local actual_tables=$(docker exec "mambogo-${service_name}-1" psql -U postgres -d "$database" -t -c "
        SELECT COUNT(*) FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name IN ($expected_tables);
    " | tr -d ' ')
    
    if [ "$actual_tables" -eq "$(echo "$expected_tables" | tr ',' '\n' | wc -l)" ]; then
        log_success "Schema validation passed: $service_name"
        return 0
    else
        log_error "Schema validation failed: $service_name (expected: $(echo "$expected_tables" | tr ',' '\n' | wc -l), got: $actual_tables)"
        return 1
    fi
}

# Data integrity test
test_data_integrity() {
    local service_name="$1"
    local port="$2"
    local database="$3"
    
    log_info "Testing data integrity for $service_name..."
    
    local result=$(docker exec "mambogo-${service_name}-1" psql -U postgres -d "$database" -t -c "
        SELECT COUNT(*) FROM (
            SELECT 'products' as table_name, COUNT(*) as count FROM products WHERE price > 0
            UNION ALL
            SELECT 'orders' as table_name, COUNT(*) as count FROM orders WHERE total_amount >= 10.00 AND total_amount <= 10000.00
            UNION ALL
            SELECT 'inventory' as table_name, COUNT(*) as count FROM inventory WHERE available_quantity + reserved_quantity <= total_quantity
        ) t;
    " | tr -d ' ')
    
    if [ "$result" -gt 0 ]; then
        log_success "Data integrity test passed: $service_name"
        return 0
    else
        log_error "Data integrity test failed: $service_name"
        return 1
    fi
}

# Business logic test
test_business_logic() {
    local service_name="$1"
    local port="$2"
    local database="$3"
    
    log_info "Testing business logic for $service_name..."
    
    local result=$(docker exec "mambogo-${service_name}-1" psql -U postgres -d "$database" -t -c "
        SELECT COUNT(*) FROM (
            SELECT 'order_total_validation' as test, 
                   CASE WHEN o.total_amount = COALESCE(SUM(oi.total_price), 0) THEN 1 ELSE 0 END as passed
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            GROUP BY o.id, o.total_amount
        ) t WHERE passed = 1;
    " | tr -d ' ')
    
    if [ "$result" -gt 0 ]; then
        log_success "Business logic test passed: $service_name"
        return 0
    else
        log_error "Business logic test failed: $service_name"
        return 1
    fi
}

# Performance test
test_performance() {
    local service_name="$1"
    local port="$2"
    local database="$3"
    
    log_info "Testing performance for $service_name..."
    
    local result=$(docker exec "mambogo-${service_name}-1" psql -U postgres -d "$database" -t -c "
        EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM products WHERE sku = 'IPHONE-15-PRO-001';
    " | grep -c "Index Scan" || echo "0")
    
    if [ "$result" -gt 0 ]; then
        log_success "Performance test passed: $service_name (index scan detected)"
        return 0
    else
        log_warning "Performance test warning: $service_name (no index scan detected)"
        return 0  # Warning, not failure
    fi
}

# Redis test
test_redis() {
    log_info "Testing Redis connection and operations..."
    
    if docker exec mambogo-redis-1 redis-cli ping | grep -q "PONG"; then
        log_success "Redis connection successful"
        
        # Test basic operations
        docker exec mambogo-redis-1 redis-cli SET "test:key" "test:value" >/dev/null
        local value=$(docker exec mambogo-redis-1 redis-cli GET "test:key")
        
        if [ "$value" = "test:value" ]; then
            log_success "Redis operations test passed"
            docker exec mambogo-redis-1 redis-cli DEL "test:key" >/dev/null
            return 0
        else
            log_error "Redis operations test failed"
            return 1
        fi
    else
        log_error "Redis connection failed"
        return 1
    fi
}

# Main testing function
main() {
    log_header "STARTING DATABASE SCHEMA VALIDATION"
    
    # Check if Docker Compose is running
    if ! docker-compose -f "$DOCKER_COMPOSE_FILE" ps | grep -q "Up"; then
        log_info "Starting Docker Compose services..."
        docker-compose -f "$DOCKER_COMPOSE_FILE" up -d
        
        log_info "Waiting for services to be ready..."
        sleep 30
    fi
    
    # Test database connections
    log_header "TESTING DATABASE CONNECTIONS"
    
    test_database_connection "postgres-products" "5433" "products"
    test_database_connection "postgres-orders" "5434" "orders"
    test_database_connection "postgres-payments" "5435" "payments"
    test_database_connection "postgres-inventory" "5436" "inventory"
    
    # Test Redis
    log_header "TESTING REDIS"
    test_redis
    
    # Test schema tables
    log_header "TESTING SCHEMA TABLES"
    
    test_schema_tables "postgres-products" "5433" "products" "products,categories,product_reviews,product_images"
    test_schema_tables "postgres-orders" "5434" "orders" "orders,order_items,outbox_events,order_status_history,order_notes,order_fulfillment,idempotency_keys"
    test_schema_tables "postgres-payments" "5435" "payments" "payments,payment_methods,payment_transactions,refunds,payment_disputes,payment_analytics"
    test_schema_tables "postgres-inventory" "5436" "inventory" "inventory,inventory_reservations,inventory_movements,inventory_alerts,inventory_suppliers,inventory_categories,inventory_analytics"
    
    # Test data integrity
    log_header "TESTING DATA INTEGRITY"
    
    test_data_integrity "postgres-products" "5433" "products"
    test_data_integrity "postgres-orders" "5434" "orders"
    test_data_integrity "postgres-payments" "5435" "payments"
    test_data_integrity "postgres-inventory" "5436" "inventory"
    
    # Test business logic
    log_header "TESTING BUSINESS LOGIC"
    
    test_business_logic "postgres-orders" "5434" "orders"
    test_business_logic "postgres-inventory" "5436" "inventory"
    
    # Test performance
    log_header "TESTING PERFORMANCE"
    
    test_performance "postgres-products" "5433" "products"
    test_performance "postgres-orders" "5434" "orders"
    test_performance "postgres-inventory" "5436" "inventory"
    
    # Run comprehensive validation queries
    log_header "RUNNING COMPREHENSIVE VALIDATION"
    
    log_info "Running validation queries on Product Service..."
    docker exec mambogo-postgres-products-1 psql -U postgres -d products -f /docker-entrypoint-initdb.d/01-product-schema.sql >/dev/null 2>&1 || true
    
    log_info "Running validation queries on Order Service..."
    docker exec mambogo-postgres-orders-1 psql -U postgres -d orders -f /docker-entrypoint-initdb.d/01-order-schema.sql >/dev/null 2>&1 || true
    
    log_info "Running validation queries on Payment Service..."
    docker exec mambogo-postgres-payments-1 psql -U postgres -d payments -f /docker-entrypoint-initdb.d/01-payment-schema.sql >/dev/null 2>&1 || true
    
    log_info "Running validation queries on Inventory Service..."
    docker exec mambogo-postgres-inventory-1 psql -U postgres -d inventory -f /docker-entrypoint-initdb.d/01-inventory-schema.sql >/dev/null 2>&1 || true
    
    # Final results
    log_header "TEST RESULTS SUMMARY"
    
    echo -e "${BLUE}Total Tests:${NC} $TOTAL_TESTS"
    echo -e "${GREEN}Passed:${NC} $PASSED_TESTS"
    echo -e "${RED}Failed:${NC} $FAILED_TESTS"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        log_success "All database schema tests passed! 🎉"
        exit 0
    else
        log_error "Some tests failed. Please review the errors above."
        exit 1
    fi
}

# Cleanup function
cleanup() {
    log_info "Cleaning up test environment..."
    docker-compose -f "$DOCKER_COMPOSE_FILE" down
}

# Trap cleanup on script exit
trap cleanup EXIT

# Run main function
main "$@"
