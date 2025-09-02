#!/bin/bash

# Security Headers Testing Script for CON-06 Implementation
# This script validates the comprehensive security headers baseline system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
TEST_ENDPOINTS=(
    "/api/products"
    "/api/cart"
    "/api/orders"
    "/api/payments"
    "/actuator/health"
)

# Required security headers (OWASP recommended)
REQUIRED_HEADERS=(
    "X-Content-Type-Options"
    "X-Frame-Options"
    "X-XSS-Protection"
    "Referrer-Policy"
)

# Critical security headers for production
CRITICAL_HEADERS=(
    "Strict-Transport-Security"
    "Content-Security-Policy"
    "Permissions-Policy"
)

# Additional security headers
ADDITIONAL_HEADERS=(
    "X-DNS-Prefetch-Control"
    "X-Permitted-Cross-Domain-Policies"
    "Server"
)

echo -e "${BLUE}🔒 Security Headers Testing Script - CON-06 Implementation${NC}"
echo "================================================================"
echo "Gateway URL: $GATEWAY_URL"
echo "Test Endpoints: ${TEST_ENDPOINTS[*]}"
echo ""

# Function to check if gateway is running
check_gateway() {
    echo -e "${BLUE}📡 Checking Gateway Service Status...${NC}"
    
    if curl -s --connect-timeout 5 "$GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Gateway service is running${NC}"
        return 0
    else
        echo -e "${RED}❌ Gateway service is not running or not accessible${NC}"
        echo "Please ensure the gateway service is running on $GATEWAY_URL"
        return 1
    fi
}

# Function to test security headers for an endpoint
test_security_headers() {
    local endpoint="$1"
    local method="${2:-GET}"
    
    echo -e "${BLUE}🔍 Testing Security Headers for $method $endpoint${NC}"
    
    # Make request and capture headers
    local response_headers
    if [ "$method" = "GET" ]; then
        response_headers=$(curl -s -I "$GATEWAY_URL$endpoint" 2>/dev/null)
    else
        response_headers=$(curl -s -I -X "$method" "$GATEWAY_URL$endpoint" 2>/dev/null)
    fi
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to connect to $endpoint${NC}"
        return 1
    fi
    
    # Check HTTP status
    local http_status=$(echo "$response_headers" | head -n 1 | cut -d' ' -f2)
    echo "  HTTP Status: $http_status"
    
    # Test required security headers
    local required_missing=0
    echo "  Required Security Headers:"
    for header in "${REQUIRED_HEADERS[@]}"; do
        if echo "$response_headers" | grep -qi "^$header:"; then
            local header_value=$(echo "$response_headers" | grep -i "^$header:" | cut -d':' -f2- | xargs)
            echo -e "    ${GREEN}✅ $header: $header_value${NC}"
        else
            echo -e "    ${RED}❌ $header: MISSING${NC}"
            ((required_missing++))
        fi
    done
    
    # Test critical security headers (warn if missing)
    echo "  Critical Security Headers:"
    for header in "${CRITICAL_HEADERS[@]}"; do
        if echo "$response_headers" | grep -qi "^$header:"; then
            local header_value=$(echo "$response_headers" | grep -i "^$header:" | cut -d':' -f2- | xargs)
            echo -e "    ${GREEN}✅ $header: $header_value${NC}"
        else
            echo -e "    ${YELLOW}⚠️  $header: MISSING (recommended for production)${NC}"
        fi
    done
    
    # Test additional security headers
    echo "  Additional Security Headers:"
    for header in "${ADDITIONAL_HEADERS[@]}"; do
        if echo "$response_headers" | grep -qi "^$header:"; then
            local header_value=$(echo "$response_headers" | grep -i "^$header:" | cut -d':' -f2- | xargs)
            echo -e "    ${GREEN}✅ $header: $header_value${NC}"
        else
            echo -e "    ${BLUE}ℹ️  $header: Not present${NC}"
        fi
    done
    
    # Check for security header values compliance
    echo "  Security Header Values Compliance:"
    check_header_values "$response_headers"
    
    echo ""
    return $required_missing
}

# Function to check security header values for compliance
check_header_values() {
    local headers="$1"
    
    # Check X-Content-Type-Options
    local content_type_opts=$(echo "$headers" | grep -i "^X-Content-Type-Options:" | cut -d':' -f2- | xargs)
    if [ "$content_type_opts" = "nosniff" ]; then
        echo -e "    ${GREEN}✅ X-Content-Type-Options: Correct value 'nosniff'${NC}"
    elif [ -n "$content_type_opts" ]; then
        echo -e "    ${YELLOW}⚠️  X-Content-Type-Options: Potentially insecure value '$content_type_opts'${NC}"
    fi
    
    # Check X-Frame-Options
    local frame_opts=$(echo "$headers" | grep -i "^X-Frame-Options:" | cut -d':' -f2- | xargs)
    if [ "$frame_opts" = "DENY" ] || [ "$frame_opts" = "SAMEORIGIN" ]; then
        echo -e "    ${GREEN}✅ X-Frame-Options: Secure value '$frame_opts'${NC}"
    elif [ -n "$frame_opts" ]; then
        echo -e "    ${YELLOW}⚠️  X-Frame-Options: Potentially insecure value '$frame_opts'${NC}"
    fi
    
    # Check X-XSS-Protection
    local xss_protection=$(echo "$headers" | grep -i "^X-XSS-Protection:" | cut -d':' -f2- | xargs)
    if [[ "$xss_protection" == 1* ]]; then
        echo -e "    ${GREEN}✅ X-XSS-Protection: Correct value '$xss_protection'${NC}"
    elif [ -n "$xss_protection" ]; then
        echo -e "    ${YELLOW}⚠️  X-XSS-Protection: Potentially insecure value '$xss_protection'${NC}"
    fi
    
    # Check Referrer-Policy
    local referrer_policy=$(echo "$headers" | grep -i "^Referrer-Policy:" | cut -d':' -f2- | xargs)
    if [ -n "$referrer_policy" ]; then
        case "$referrer_policy" in
            "no-referrer"|"no-referrer-when-downgrade"|"origin"|"origin-when-cross-origin"|"same-origin"|"strict-origin"|"strict-origin-when-cross-origin"|"unsafe-url")
                echo -e "    ${GREEN}✅ Referrer-Policy: Valid value '$referrer_policy'${NC}"
                ;;
            *)
                echo -e "    ${YELLOW}⚠️  Referrer-Policy: Potentially insecure value '$referrer_policy'${NC}"
                ;;
        esac
    fi
}

# Function to test CORS preflight requests
test_cors_preflight() {
    echo -e "${BLUE}🔄 Testing CORS Preflight Requests...${NC}"
    
    for endpoint in "${TEST_ENDPOINTS[@]}"; do
        if [[ "$endpoint" != "/actuator/health" ]]; then
            echo "  Testing OPTIONS $endpoint"
            
            local preflight_response
            preflight_response=$(curl -s -I -X OPTIONS \
                -H "Origin: http://localhost:5173" \
                -H "Access-Control-Request-Method: GET" \
                -H "Access-Control-Request-Headers: Content-Type" \
                "$GATEWAY_URL$endpoint" 2>/dev/null)
            
            if [ $? -eq 0 ]; then
                local cors_origin=$(echo "$preflight_response" | grep -i "^Access-Control-Allow-Origin:" | cut -d':' -f2- | xargs)
                local cors_methods=$(echo "$preflight_response" | grep -i "^Access-Control-Allow-Methods:" | cut -d':' -f2- | xargs)
                
                if [ -n "$cors_origin" ]; then
                    echo -e "    ${GREEN}✅ CORS Origin: $cors_origin${NC}"
                else
                    echo -e "    ${RED}❌ CORS Origin: Missing${NC}"
                fi
                
                if [ -n "$cors_methods" ]; then
                    echo -e "    ${GREEN}✅ CORS Methods: $cors_methods${NC}"
                else
                    echo -e "    ${RED}❌ CORS Methods: Missing${NC}"
                fi
            else
                echo -e "    ${RED}❌ CORS Preflight failed${NC}"
            fi
        fi
    done
    echo ""
}

# Function to test security headers with different origins
test_origin_based_headers() {
    echo -e "${BLUE}🌐 Testing Origin-Based Security Headers...${NC}"
    
    local test_origins=(
        "http://localhost:5173"
        "http://localhost:3000"
        "https://malicious-site.com"
        "http://192.168.1.100:8080"
    )
    
    for origin in "${test_origins[@]}"; do
        echo "  Testing with Origin: $origin"
        
        local response_headers
        response_headers=$(curl -s -I -H "Origin: $origin" "$GATEWAY_URL/api/products" 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            # Check if security headers are still present
            local security_headers_present=0
            for header in "${REQUIRED_HEADERS[@]}"; do
                if echo "$response_headers" | grep -qi "^$header:"; then
                    ((security_headers_present++))
                fi
            done
            
            if [ $security_headers_present -eq ${#REQUIRED_HEADERS[@]} ]; then
                echo -e "    ${GREEN}✅ All required security headers present${NC}"
            else
                echo -e "    ${RED}❌ Missing security headers for origin: $origin${NC}"
            fi
        else
            echo -e "    ${RED}❌ Request failed for origin: $origin${NC}"
        fi
    done
    echo ""
}

# Function to test performance impact
test_performance() {
    echo -e "${BLUE}⚡ Testing Security Headers Performance Impact...${NC}"
    
    local iterations=10
    local total_time=0
    
    echo "  Running $iterations requests to measure performance impact..."
    
    for i in $(seq 1 $iterations); do
        local start_time=$(date +%s%N)
        curl -s "$GATEWAY_URL/api/products" > /dev/null 2>&1
        local end_time=$(date +%s%N)
        
        local duration=$(( (end_time - start_time) / 1000000 ))  # Convert to milliseconds
        total_time=$((total_time + duration))
        
        echo "    Request $i: ${duration}ms"
    done
    
    local avg_time=$((total_time / iterations))
    echo "  Average response time: ${avg_time}ms"
    
    if [ $avg_time -lt 100 ]; then
        echo -e "    ${GREEN}✅ Performance impact: Minimal (<100ms)${NC}"
    elif [ $avg_time -lt 500 ]; then
        echo -e "    ${YELLOW}⚠️  Performance impact: Moderate (100-500ms)${NC}"
    else
        echo -e "    ${RED}❌ Performance impact: High (>500ms)${NC}"
    fi
    echo ""
}

# Function to generate test report
generate_report() {
    local total_tests=$1
    local passed_tests=$2
    local failed_tests=$3
    
    echo -e "${BLUE}📊 Security Headers Test Report${NC}"
    echo "================================="
    echo "Total Tests: $total_tests"
    echo -e "Passed: ${GREEN}$passed_tests${NC}"
    echo -e "Failed: ${RED}$failed_tests${NC}"
    
    local success_rate=$(( (passed_tests * 100) / total_tests ))
    echo "Success Rate: ${success_rate}%"
    
    if [ $success_rate -ge 90 ]; then
        echo -e "${GREEN}🎉 Excellent! Security headers are working correctly${NC}"
    elif [ $success_rate -ge 80 ]; then
        echo -e "${YELLOW}⚠️  Good! Some security headers need attention${NC}"
    else
        echo -e "${RED}❌ Critical! Security headers need immediate attention${NC}"
    fi
}

# Main test execution
main() {
    echo -e "${BLUE}🚀 Starting Security Headers Testing...${NC}"
    echo ""
    
    # Check if gateway is running
    if ! check_gateway; then
        exit 1
    fi
    
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    # Test security headers for each endpoint
    for endpoint in "${TEST_ENDPOINTS[@]}"; do
        if test_security_headers "$endpoint"; then
            ((passed_tests++))
        else
            ((failed_tests++))
        fi
        ((total_tests++))
    done
    
    # Test CORS preflight
    test_cors_preflight
    
    # Test origin-based headers
    test_origin_based_headers
    
    # Test performance
    test_performance
    
    # Generate final report
    generate_report $total_tests $passed_tests $failed_tests
    
    # Exit with appropriate code
    if [ $failed_tests -eq 0 ]; then
        echo -e "${GREEN}✅ All security headers tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}❌ Some security headers tests failed${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
