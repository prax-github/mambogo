#!/bin/bash

# SEC-09 CSP Policy Testing Script
# Tests Content Security Policy implementation across different environments
# Author: Prashant Sinha

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
TEST_TIMEOUT=10
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test Results
declare -a TEST_RESULTS=()

echo -e "${BLUE}=== SEC-09 CSP Policy Testing Suite ===${NC}"
echo "Testing Content Security Policy implementation"
echo "Gateway URL: $GATEWAY_URL"
echo "Timeout: ${TEST_TIMEOUT}s"
echo

# Function to print test results
print_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC} - $test_name"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("PASS: $test_name")
    else
        echo -e "${RED}✗ FAIL${NC} - $test_name"
        [ -n "$details" ] && echo -e "  ${YELLOW}Details: $details${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("FAIL: $test_name - $details")
    fi
}

# Function to test CSP header presence
test_csp_header_presence() {
    local endpoint="$1"
    local expected_header="$2"
    
    echo "Testing CSP header presence on $endpoint..."
    
    response=$(curl -s -I --max-time $TEST_TIMEOUT "$GATEWAY_URL$endpoint" 2>/dev/null)
    status=$?
    
    if [ $status -ne 0 ]; then
        print_result "CSP Header ($endpoint)" "FAIL" "Connection failed (status: $status)"
        return
    fi
    
    if echo "$response" | grep -qi "$expected_header"; then
        print_result "CSP Header ($endpoint)" "PASS"
    else
        print_result "CSP Header ($endpoint)" "FAIL" "Missing $expected_header header"
    fi
}

# Function to test CSP policy content
test_csp_policy_content() {
    local endpoint="$1"
    local expected_directive="$2"
    
    echo "Testing CSP policy content on $endpoint for directive: $expected_directive..."
    
    csp_header=$(curl -s -I --max-time $TEST_TIMEOUT "$GATEWAY_URL$endpoint" 2>/dev/null | grep -i "content-security-policy" | head -1)
    
    if [ -z "$csp_header" ]; then
        print_result "CSP Policy Content ($expected_directive)" "FAIL" "No CSP header found"
        return
    fi
    
    if echo "$csp_header" | grep -qi "$expected_directive"; then
        print_result "CSP Policy Content ($expected_directive)" "PASS"
    else
        print_result "CSP Policy Content ($expected_directive)" "FAIL" "Missing directive: $expected_directive"
    fi
}

# Function to test CSP violation reporting endpoint
test_csp_violation_endpoint() {
    echo "Testing CSP violation reporting endpoint..."
    
    # Test CSP violation report endpoint
    violation_payload='{
        "csp-report": {
            "document-uri": "https://example.com/test",
            "violated-directive": "script-src",
            "blocked-uri": "https://evil.com/script.js",
            "original-policy": "default-src '\''self'\''; script-src '\''self'\''"
        }
    }'
    
    response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$violation_payload" \
        --max-time $TEST_TIMEOUT \
        "$GATEWAY_URL/api/csp/violations" 2>/dev/null)
    status=$?
    
    if [ $status -eq 0 ] && echo "$response" | grep -qi "success\|processed"; then
        print_result "CSP Violation Endpoint" "PASS"
    else
        print_result "CSP Violation Endpoint" "FAIL" "Violation reporting failed"
    fi
}

# Function to test CSP health endpoint
test_csp_health_endpoint() {
    echo "Testing CSP health endpoint..."
    
    response=$(curl -s --max-time $TEST_TIMEOUT "$GATEWAY_URL/api/csp/health" 2>/dev/null)
    status=$?
    
    if [ $status -eq 0 ] && echo "$response" | grep -qi "healthy\|status"; then
        print_result "CSP Health Endpoint" "PASS"
    else
        print_result "CSP Health Endpoint" "FAIL" "Health check failed"
    fi
}

# Function to test CSP metrics endpoint
test_csp_metrics_endpoint() {
    echo "Testing CSP metrics endpoint..."
    
    response=$(curl -s --max-time $TEST_TIMEOUT "$GATEWAY_URL/api/csp/metrics" 2>/dev/null)
    status=$?
    
    if [ $status -eq 0 ] && echo "$response" | grep -qi "summary\|metrics"; then
        print_result "CSP Metrics Endpoint" "PASS"
    else
        print_result "CSP Metrics Endpoint" "FAIL" "Metrics endpoint failed"
    fi
}

# Function to test security headers complement
test_security_headers() {
    local endpoint="$1"
    
    echo "Testing security headers on $endpoint..."
    
    response=$(curl -s -I --max-time $TEST_TIMEOUT "$GATEWAY_URL$endpoint" 2>/dev/null)
    
    # Check for various security headers
    headers=("X-Content-Type-Options" "X-Frame-Options" "X-XSS-Protection" "Referrer-Policy")
    
    for header in "${headers[@]}"; do
        if echo "$response" | grep -qi "$header"; then
            print_result "Security Header ($header)" "PASS"
        else
            print_result "Security Header ($header)" "FAIL" "Missing $header header"
        fi
    done
}

# Function to test CORS integration with CSP
test_cors_csp_integration() {
    echo "Testing CORS and CSP integration..."
    
    # Test CORS request with CSP headers
    response=$(curl -s -I \
        -H "Origin: http://localhost:5173" \
        -H "Access-Control-Request-Method: GET" \
        --max-time $TEST_TIMEOUT \
        "$GATEWAY_URL/api/products" 2>/dev/null)
    
    has_cors=$(echo "$response" | grep -i "access-control-allow-origin")
    has_csp=$(echo "$response" | grep -i "content-security-policy")
    
    if [ -n "$has_cors" ] && [ -n "$has_csp" ]; then
        print_result "CORS-CSP Integration" "PASS"
    else
        print_result "CORS-CSP Integration" "FAIL" "Missing CORS or CSP headers"
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    echo "Waiting for gateway service to be ready..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s --max-time 5 "$GATEWAY_URL/actuator/health" >/dev/null 2>&1; then
            echo -e "${GREEN}Service is ready!${NC}"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts - Service not ready, waiting..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}Service failed to become ready after $max_attempts attempts${NC}"
    return 1
}

# Main test execution
main() {
    echo -e "${BLUE}Starting CSP Policy Tests...${NC}"
    echo
    
    # Wait for service to be ready
    if ! wait_for_service; then
        echo -e "${RED}Cannot run tests - service is not available${NC}"
        exit 1
    fi
    
    echo
    echo -e "${BLUE}=== CSP Header Tests ===${NC}"
    
    # Test CSP headers on various endpoints
    test_csp_header_presence "/api/products" "Content-Security-Policy"
    test_csp_header_presence "/actuator/health" "Content-Security-Policy"
    
    echo
    echo -e "${BLUE}=== CSP Policy Content Tests ===${NC}"
    
    # Test specific CSP directives
    test_csp_policy_content "/api/products" "default-src"
    test_csp_policy_content "/api/products" "script-src"
    test_csp_policy_content "/api/products" "style-src"
    test_csp_policy_content "/api/products" "object-src"
    test_csp_policy_content "/api/products" "frame-ancestors"
    
    echo
    echo -e "${BLUE}=== CSP Violation Reporting Tests ===${NC}"
    
    # Test CSP violation reporting
    test_csp_violation_endpoint
    test_csp_health_endpoint
    test_csp_metrics_endpoint
    
    echo
    echo -e "${BLUE}=== Security Headers Tests ===${NC}"
    
    # Test complementary security headers
    test_security_headers "/api/products"
    
    echo
    echo -e "${BLUE}=== Integration Tests ===${NC}"
    
    # Test CORS and CSP integration
    test_cors_csp_integration
    
    echo
    echo -e "${BLUE}=== Test Summary ===${NC}"
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}All tests passed! 🎉${NC}"
        exit 0
    else
        echo -e "${RED}Some tests failed. Check the details above.${NC}"
        echo
        echo -e "${YELLOW}Failed Tests:${NC}"
        for result in "${TEST_RESULTS[@]}"; do
            if [[ $result == FAIL* ]]; then
                echo -e "  ${RED}• ${result#FAIL: }${NC}"
            fi
        done
        exit 1
    fi
}

# Run the tests
main "$@"
