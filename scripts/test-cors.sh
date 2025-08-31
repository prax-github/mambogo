#!/bin/bash

# SEC-06 CORS Configuration Testing Script
# Tests CORS functionality across different environments and scenarios
# Author: Prashant Sinha

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TEST_RESULTS_FILE="cors-test-results-$(date +%Y%m%d_%H%M%S).log"

echo -e "${BLUE}🔒 SEC-06 CORS Configuration Testing${NC}"
echo "========================================"
echo "Gateway URL: $GATEWAY_URL"
echo "Results will be logged to: $TEST_RESULTS_FILE"
echo ""

# Initialize results file
echo "CORS Testing Results - $(date)" > "$TEST_RESULTS_FILE"
echo "Gateway URL: $GATEWAY_URL" >> "$TEST_RESULTS_FILE"
echo "========================================" >> "$TEST_RESULTS_FILE"

# Test counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run a CORS test
run_cors_test() {
    local test_name="$1"
    local origin="$2"
    local method="${3:-GET}"
    local endpoint="${4:-/api/products}"
    local should_pass="${5:-true}"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${YELLOW}Testing: $test_name${NC}"
    echo "  Origin: $origin"
    echo "  Method: $method"
    echo "  Endpoint: $endpoint"
    
    # Prepare curl command
    local curl_cmd="curl -s -w '%{http_code}' -o /dev/null"
    
    if [ "$method" = "OPTIONS" ]; then
        curl_cmd="$curl_cmd -X OPTIONS"
        curl_cmd="$curl_cmd -H 'Access-Control-Request-Method: POST'"
        curl_cmd="$curl_cmd -H 'Access-Control-Request-Headers: Authorization,Content-Type'"
    fi
    
    if [ "$origin" != "none" ]; then
        curl_cmd="$curl_cmd -H 'Origin: $origin'"
    fi
    
    curl_cmd="$curl_cmd '$GATEWAY_URL$endpoint'"
    
    # Execute test
    local response_code
    response_code=$(eval "$curl_cmd")
    
    # Check result
    local test_passed=false
    if [ "$should_pass" = "true" ]; then
        if [ "$response_code" -ge 200 ] && [ "$response_code" -lt 400 ]; then
            test_passed=true
        fi
    else
        if [ "$response_code" -ge 400 ]; then
            test_passed=true
        fi
    fi
    
    # Log result
    if [ "$test_passed" = true ]; then
        echo -e "  ${GREEN}✓ PASSED${NC} (HTTP $response_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "PASS: $test_name - HTTP $response_code" >> "$TEST_RESULTS_FILE"
    else
        echo -e "  ${RED}✗ FAILED${NC} (HTTP $response_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "FAIL: $test_name - HTTP $response_code" >> "$TEST_RESULTS_FILE"
    fi
    
    echo ""
}

# Function to test CORS headers
test_cors_headers() {
    local test_name="$1"
    local origin="$2"
    local expected_origin="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${YELLOW}Testing CORS Headers: $test_name${NC}"
    echo "  Origin: $origin"
    echo "  Expected Access-Control-Allow-Origin: $expected_origin"
    
    # Get CORS headers
    local headers_output
    headers_output=$(curl -s -I -H "Origin: $origin" -X OPTIONS \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Authorization,Content-Type" \
        "$GATEWAY_URL/api/products" 2>/dev/null || echo "")
    
    # Check for Access-Control-Allow-Origin header
    local allow_origin_header
    allow_origin_header=$(echo "$headers_output" | grep -i "access-control-allow-origin" | head -1)
    
    if echo "$allow_origin_header" | grep -q "$expected_origin"; then
        echo -e "  ${GREEN}✓ PASSED${NC} - Correct CORS headers"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "PASS: CORS Headers $test_name" >> "$TEST_RESULTS_FILE"
    else
        echo -e "  ${RED}✗ FAILED${NC} - Incorrect or missing CORS headers"
        echo "  Received: $allow_origin_header"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "FAIL: CORS Headers $test_name - $allow_origin_header" >> "$TEST_RESULTS_FILE"
    fi
    
    echo ""
}

echo -e "${BLUE}🧪 Starting CORS Tests...${NC}"
echo ""

# Test 1: Valid local development origins
run_cors_test "Local Dev - Vite Server" "http://localhost:5173" "GET" "/api/products" "true"
run_cors_test "Local Dev - React Server" "http://localhost:3000" "GET" "/api/products" "true"
run_cors_test "Local Dev - Vite Preview" "http://localhost:4173" "GET" "/api/products" "true"

# Test 2: Valid production origins
run_cors_test "Production - WWW Domain" "https://www.mambogo.com" "GET" "/api/products" "true"
run_cors_test "Production - Apex Domain" "https://mambogo.com" "GET" "/api/products" "true"

# Test 3: Invalid origins (should fail)
run_cors_test "Invalid Origin - Random Domain" "https://evil.com" "GET" "/api/products" "false"
run_cors_test "Invalid Origin - HTTP in Prod" "http://www.mambogo.com" "GET" "/api/products" "false"
run_cors_test "Invalid Origin - Suspicious" "null" "GET" "/api/products" "false"

# Test 4: Preflight requests (OPTIONS)
run_cors_test "Preflight - Valid Origin" "http://localhost:5173" "OPTIONS" "/api/cart" "true"
run_cors_test "Preflight - Invalid Origin" "https://evil.com" "OPTIONS" "/api/cart" "false"

# Test 5: Different HTTP methods
run_cors_test "POST Request - Valid Origin" "http://localhost:5173" "POST" "/api/cart" "true"
run_cors_test "PUT Request - Valid Origin" "http://localhost:5173" "PUT" "/api/cart" "true"
run_cors_test "DELETE Request - Valid Origin" "http://localhost:5173" "DELETE" "/api/cart" "true"

# Test 6: Different endpoints
run_cors_test "Public Endpoint" "http://localhost:5173" "GET" "/api/products" "true"
run_cors_test "Secured Endpoint" "http://localhost:5173" "GET" "/api/cart" "true"
run_cors_test "Admin Endpoint" "http://localhost:5173" "GET" "/api/admin/users" "true"

# Test 7: CORS headers validation
test_cors_headers "Local Development" "http://localhost:5173" "http://localhost:5173"
test_cors_headers "Production WWW" "https://www.mambogo.com" "https://www.mambogo.com"

# Test 8: No origin header (direct API access)
run_cors_test "No Origin Header" "none" "GET" "/api/products" "true"

echo -e "${BLUE}📊 Test Results Summary${NC}"
echo "======================="
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""

# Write summary to file
echo "" >> "$TEST_RESULTS_FILE"
echo "SUMMARY:" >> "$TEST_RESULTS_FILE"
echo "Total Tests: $TOTAL_TESTS" >> "$TEST_RESULTS_FILE"
echo "Passed: $PASSED_TESTS" >> "$TEST_RESULTS_FILE"
echo "Failed: $FAILED_TESTS" >> "$TEST_RESULTS_FILE"

# Exit with appropriate code
if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All CORS tests passed!${NC}"
    echo "Results saved to: $TEST_RESULTS_FILE"
    exit 0
else
    echo -e "${RED}❌ Some CORS tests failed!${NC}"
    echo "Check results in: $TEST_RESULTS_FILE"
    exit 1
fi
