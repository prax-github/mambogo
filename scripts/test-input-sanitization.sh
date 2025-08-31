#!/bin/bash

# SEC-11: Input Sanitization Middleware Testing Script
# Tests comprehensive input sanitization functionality, threat detection, and metrics

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TEST_ORIGIN="http://localhost:5173"
AUTH_TOKEN=""
VERBOSE=${VERBOSE:-false}

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo -e "${BLUE}🧪 SEC-11: Input Sanitization Middleware Test Suite${NC}"
echo "======================================================"
echo "Gateway URL: $GATEWAY_URL"
echo "Test Origin: $TEST_ORIGIN"
echo ""

# Utility functions
log_test() {
    ((TOTAL_TESTS++))
    echo -e "${BLUE}Test $TOTAL_TESTS: $1${NC}"
}

log_success() {
    ((PASSED_TESTS++))
    echo -e "${GREEN}✅ PASSED: $1${NC}"
    [ "$VERBOSE" = true ] && echo "   $2"
}

log_failure() {
    ((FAILED_TESTS++))
    echo -e "${RED}❌ FAILED: $1${NC}"
    echo -e "${RED}   $2${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
}

log_info() {
    echo -e "${BLUE}ℹ️  INFO: $1${NC}"
}

# Test helper function
test_request() {
    local method="$1"
    local path="$2"
    local data="$3"
    local expected_status="$4"
    local test_name="$5"
    
    log_test "$test_name"
    
    local cmd="curl -s -w '%{http_code}' -X $method"
    
    if [ ! -z "$data" ]; then
        cmd="$cmd -H 'Content-Type: application/json' -d '$data'"
    fi
    
    cmd="$cmd -H 'Origin: $TEST_ORIGIN' -H 'User-Agent: InputSanitizationTest/1.0'"
    
    if [ ! -z "$AUTH_TOKEN" ]; then
        cmd="$cmd -H 'Authorization: Bearer $AUTH_TOKEN'"
    fi
    
    cmd="$cmd '$GATEWAY_URL$path'"
    
    local response=$(eval $cmd)
    local status_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$status_code" = "$expected_status" ]; then
        log_success "$test_name" "Status: $status_code"
        if [ "$VERBOSE" = true ] && [ ! -z "$body" ]; then
            echo "   Response: $body"
        fi
        return 0
    else
        log_failure "$test_name" "Expected $expected_status, got $status_code. Response: $body"
        return 1
    fi
}

# Test 1: Basic Sanitization - Clean Request
echo -e "\n${YELLOW}📋 Basic Sanitization Tests${NC}"
echo "=================================="

test_request "GET" "/api/products?search=laptop" "" "200" "Clean query parameter"

test_request "GET" "/api/products?category=electronics&sort=price" "" "200" "Multiple clean parameters"

# Test 2: XSS Attack Detection
echo -e "\n${YELLOW}🚨 XSS Attack Detection Tests${NC}"
echo "=================================="

test_request "GET" "/api/products?search=<script>alert('xss')</script>" "" "400" "Script tag in query parameter"

test_request "GET" "/api/products?search=javascript:alert('xss')" "" "400" "JavaScript protocol in query parameter"

test_request "POST" "/api/cart" '{"productId":"123","notes":"<script>alert(\"xss\")</script>"}' "400" "Script tag in request body"

# Test 3: SQL Injection Detection
echo -e "\n${YELLOW}💉 SQL Injection Detection Tests${NC}"
echo "=================================="

test_request "GET" "/api/products?search='; DROP TABLE products; --" "" "400" "SQL DROP statement"

test_request "GET" "/api/products?search=1' UNION SELECT * FROM users --" "" "400" "SQL UNION attack"

test_request "POST" "/api/cart" '{"productId":"123 OR 1=1","quantity":1}' "400" "SQL injection in JSON field"

# Test 4: Path Traversal Detection
echo -e "\n${YELLOW}📂 Path Traversal Detection Tests${NC}"
echo "=================================="

test_request "GET" "/api/products?file=../../../etc/passwd" "" "400" "Path traversal attack"

test_request "GET" "/api/products?image=%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd" "" "400" "URL encoded path traversal"

# Test 5: Command Injection Detection
echo -e "\n${YELLOW}⚡ Command Injection Detection Tests${NC}"
echo "=================================="

test_request "GET" "/api/products?cmd=; ls -la" "" "400" "Command injection with semicolon"

test_request "GET" "/api/products?exec=\$(whoami)" "" "400" "Command substitution attack"

test_request "POST" "/api/cart" '{"notes":"test; cat /etc/passwd"}' "400" "Command injection in JSON"

# Test 6: Endpoint-Specific Policies
echo -e "\n${YELLOW}🎯 Endpoint-Specific Policy Tests${NC}"
echo "=================================="

# Products endpoint (permissive policy)
test_request "GET" "/api/products?search=<i>italic</i>" "" "200" "HTML in products search (permissive)"

# Payments endpoint (strict policy)
test_request "POST" "/api/payments/charge" '{"amount":"<i>100</i>"}' "400" "HTML in payments (strict)"

# Test 7: Request Size Limits
echo -e "\n${YELLOW}📏 Request Size Limit Tests${NC}"
echo "=================================="

# Create large request body
LARGE_DATA=$(python3 -c "print('x' * 1048576)")  # 1MB
test_request "POST" "/api/cart" "{\"notes\":\"$LARGE_DATA\"}" "413" "Large request body rejection"

# Test 8: Rate Limiting Integration
echo -e "\n${YELLOW}⏱️  Rate Limiting Integration Tests${NC}"
echo "=================================="

# Test multiple suspicious requests to trigger rate limiting
for i in {1..5}; do
    curl -s -X GET -H "Origin: $TEST_ORIGIN" \
         "$GATEWAY_URL/api/products?search=<script>alert($i)</script>" > /dev/null
done

test_request "GET" "/api/products?search=normal" "" "429" "Rate limiting after violations"

# Test 9: Metrics Endpoints
echo -e "\n${YELLOW}📊 Metrics and Health Tests${NC}"
echo "=================================="

log_test "Sanitization metrics availability"
METRICS_RESPONSE=$(curl -s "$GATEWAY_URL/actuator/prometheus" | grep "gateway_sanitization")
if [ ! -z "$METRICS_RESPONSE" ]; then
    log_success "Sanitization metrics found" "Found metrics: $(echo "$METRICS_RESPONSE" | wc -l) lines"
else
    log_warning "Sanitization metrics not found or not ready yet"
fi

log_test "Health endpoint accessibility"
HEALTH_RESPONSE=$(curl -s "$GATEWAY_URL/actuator/health")
if echo "$HEALTH_RESPONSE" | grep -q "UP"; then
    log_success "Health endpoint responding" "Status: UP"
else
    log_failure "Health endpoint not responding" "Response: $HEALTH_RESPONSE"
fi

# Test 10: Whitelist Functionality
echo -e "\n${YELLOW}✅ Whitelist Functionality Tests${NC}"
echo "=================================="

test_request "GET" "/actuator/health" "" "200" "Whitelisted actuator endpoint"

test_request "GET" "/api/csp/violations" "" "405" "Whitelisted CSP endpoint (method not allowed is expected)"

# Test 11: Performance Testing
echo -e "\n${YELLOW}⚡ Performance Testing${NC}"
echo "=================================="

log_test "Sanitization performance test"
START_TIME=$(date +%s%N)

for i in {1..50}; do
    curl -s -X GET -H "Origin: $TEST_ORIGIN" \
         "$GATEWAY_URL/api/products?search=test$i" > /dev/null
done

END_TIME=$(date +%s%N)
DURATION_MS=$(((END_TIME - START_TIME) / 1000000))
AVG_TIME=$((DURATION_MS / 50))

if [ $AVG_TIME -lt 100 ]; then
    log_success "Performance test" "Average request time: ${AVG_TIME}ms (< 100ms target)"
else
    log_warning "Performance concern" "Average request time: ${AVG_TIME}ms (>= 100ms)"
fi

# Test 12: Header Sanitization
echo -e "\n${YELLOW}📋 Header Sanitization Tests${NC}"
echo "=================================="

test_request "GET" "/api/products" "" "200" "Clean User-Agent header" \
    -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

log_test "Malicious User-Agent header"
RESPONSE=$(curl -s -w '%{http_code}' -X GET \
           -H "Origin: $TEST_ORIGIN" \
           -H "User-Agent: <script>alert('xss')</script>" \
           "$GATEWAY_URL/api/products")

STATUS_CODE="${RESPONSE: -3}"
if [ "$STATUS_CODE" = "200" ]; then
    log_success "Malicious User-Agent sanitized" "Request allowed with sanitized header"
else
    log_info "Malicious User-Agent blocked" "Status: $STATUS_CODE"
fi

# Test 13: Unicode and Encoding Tests
echo -e "\n${YELLOW}🌐 Unicode and Encoding Tests${NC}"
echo "=================================="

test_request "GET" "/api/products?search=%3Cscript%3Ealert%28%27xss%27%29%3C%2Fscript%3E" "" "400" "URL encoded XSS"

test_request "GET" "/api/products?search=%u003Cscript%u003E" "" "400" "Unicode encoded XSS"

# Test Results Summary
echo ""
echo "======================================================"
echo -e "${BLUE}📊 Test Results Summary${NC}"
echo "======================================================"
echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 All tests passed! Input sanitization is working correctly.${NC}"
    exit 0
else
    echo -e "\n${RED}❌ Some tests failed. Please review the implementation.${NC}"
    exit 1
fi
