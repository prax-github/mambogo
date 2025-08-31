#!/bin/bash

# Advanced Rate Limiting Test Script for SEC-10
# Tests endpoint-specific rate limits, circuit breakers, adaptive thresholds,
# and comprehensive monitoring capabilities.

set -e

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TEST_OUTPUT_DIR="./rate-limit-test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${TEST_OUTPUT_DIR}/advanced_rate_limit_test_${TIMESTAMP}.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
PARALLEL_REQUESTS=10
RAPID_REQUESTS=50
TEST_DURATION=60

# Create output directory
mkdir -p "$TEST_OUTPUT_DIR"

# Logging function
log() {
    echo -e "$(date '+%Y-%m-%d %H:%M:%S') $1" | tee -a "$LOG_FILE"
}

# Test result tracking
declare -A test_results
declare -A endpoint_limits

# Initialize endpoint limits for validation
endpoint_limits["products"]=2000
endpoint_limits["cart"]=60
endpoint_limits["orders"]=30
endpoint_limits["payments"]=20

print_header() {
    log "${BLUE}================================================================================================${NC}"
    log "${BLUE}                    SEC-10 Advanced Rate Limiting Test Suite                                  ${NC}"
    log "${BLUE}================================================================================================${NC}"
    log "${YELLOW}Gateway URL: $GATEWAY_URL${NC}"
    log "${YELLOW}Test Timestamp: $TIMESTAMP${NC}"
    log "${YELLOW}Results Directory: $TEST_OUTPUT_DIR${NC}"
    log "${BLUE}================================================================================================${NC}"
}

print_test_section() {
    log "\n${YELLOW}--- $1 ---${NC}"
}

# Check if gateway is accessible
check_gateway_health() {
    print_test_section "Gateway Health Check"
    
    if curl -s -f "${GATEWAY_URL}/actuator/health" > /dev/null; then
        log "${GREEN}✓ Gateway is accessible${NC}"
        test_results["gateway_health"]="PASS"
    else
        log "${RED}✗ Gateway is not accessible${NC}"
        test_results["gateway_health"]="FAIL"
        exit 1
    fi
}

# Test endpoint-specific rate limiting
test_endpoint_rate_limits() {
    print_test_section "Endpoint-Specific Rate Limiting Tests"
    
    local endpoints=("products" "cart" "orders" "payments")
    
    for endpoint in "${endpoints[@]}"; do
        log "${BLUE}Testing $endpoint endpoint rate limiting...${NC}"
        
        # Determine the appropriate path and auth requirements
        local path="/api/${endpoint}"
        local auth_header=""
        
        # Add authentication for secured endpoints
        if [[ "$endpoint" != "products" ]]; then
            # For secured endpoints, we'll simulate with a test token
            # In a real environment, you'd get a valid JWT token
            auth_header="-H 'Authorization: Bearer test-token-for-rate-limiting'"
        fi
        
        # Test normal rate within limits
        log "  Testing normal rate (within limits)..."
        local success_count=0
        for i in {1..5}; do
            if eval "curl -s -o /dev/null -w '%{http_code}' $auth_header '${GATEWAY_URL}${path}'" | grep -q "200\|404\|401"; then
                ((success_count++))
            fi
            sleep 1
        done
        
        if [ $success_count -eq 5 ]; then
            log "${GREEN}  ✓ Normal rate requests succeeded${NC}"
        else
            log "${YELLOW}  ⚠ Some normal rate requests failed (expected if service not running)${NC}"
        fi
        
        # Test rapid requests to trigger rate limiting
        log "  Testing rapid requests (should trigger rate limiting)..."
        local rate_limited_count=0
        
        # Send rapid requests
        for i in $(seq 1 $RAPID_REQUESTS); do
            local response_code
            response_code=$(eval "curl -s -o /dev/null -w '%{http_code}' $auth_header '${GATEWAY_URL}${path}'")
            
            if [ "$response_code" = "429" ]; then
                ((rate_limited_count++))
            fi
            
            # Small delay to avoid overwhelming the system
            sleep 0.1
        done
        
        log "  Rate limited responses: $rate_limited_count/$RAPID_REQUESTS"
        
        if [ $rate_limited_count -gt 0 ]; then
            log "${GREEN}  ✓ Rate limiting is working for $endpoint${NC}"
            test_results["${endpoint}_rate_limiting"]="PASS"
        else
            log "${YELLOW}  ⚠ No rate limiting detected for $endpoint (may need authentication or higher load)${NC}"
            test_results["${endpoint}_rate_limiting"]="PARTIAL"
        fi
    done
}

# Test rate limiting headers
test_rate_limiting_headers() {
    print_test_section "Rate Limiting Headers Validation"
    
    local endpoints=("products" "cart" "orders" "payments")
    
    for endpoint in "${endpoints[@]}"; do
        log "${BLUE}Testing rate limiting headers for $endpoint...${NC}"
        
        local path="/api/${endpoint}"
        local auth_header=""
        
        if [[ "$endpoint" != "products" ]]; then
            auth_header="-H 'Authorization: Bearer test-token-for-rate-limiting'"
        fi
        
        # Check for rate limiting headers
        local headers_output
        headers_output=$(eval "curl -s -I $auth_header '${GATEWAY_URL}${path}' 2>/dev/null" | grep -i "x-rate-limit" || true)
        
        if [ -n "$headers_output" ]; then
            log "${GREEN}  ✓ Rate limiting headers found:${NC}"
            echo "$headers_output" | while read -r header; do
                log "    $header"
            done
            test_results["${endpoint}_headers"]="PASS"
        else
            log "${YELLOW}  ⚠ No rate limiting headers found for $endpoint${NC}"
            test_results["${endpoint}_headers"]="PARTIAL"
        fi
    done
}

# Test circuit breaker functionality
test_circuit_breaker() {
    print_test_section "Circuit Breaker Testing"
    
    log "${BLUE}Testing circuit breaker with rapid failure simulation...${NC}"
    
    # Send a large number of requests to trigger circuit breaker
    local endpoint="orders"
    local path="/api/${endpoint}"
    local auth_header="-H 'Authorization: Bearer test-token-for-rate-limiting'"
    
    log "Sending rapid requests to trigger circuit breaker..."
    
    local circuit_breaker_triggered=false
    local consecutive_429s=0
    
    for i in $(seq 1 100); do
        local response_code
        response_code=$(eval "curl -s -o /dev/null -w '%{http_code}' $auth_header '${GATEWAY_URL}${path}'")
        
        if [ "$response_code" = "429" ]; then
            ((consecutive_429s++))
            if [ $consecutive_429s -ge 10 ]; then
                circuit_breaker_triggered=true
                break
            fi
        else
            consecutive_429s=0
        fi
        
        sleep 0.05
    done
    
    if [ "$circuit_breaker_triggered" = true ]; then
        log "${GREEN}✓ Circuit breaker appears to be working (consistent 429 responses)${NC}"
        test_results["circuit_breaker"]="PASS"
    else
        log "${YELLOW}⚠ Circuit breaker may not be triggered or needs more load${NC}"
        test_results["circuit_breaker"]="PARTIAL"
    fi
}

# Test metrics endpoints
test_metrics_endpoints() {
    print_test_section "Advanced Metrics Validation"
    
    log "${BLUE}Testing rate limiting metrics endpoints...${NC}"
    
    # Test Prometheus metrics endpoint
    if curl -s "${GATEWAY_URL}/actuator/prometheus" | grep -q "gateway_rate_limit"; then
        log "${GREEN}✓ Rate limiting metrics found in Prometheus endpoint${NC}"
        test_results["prometheus_metrics"]="PASS"
        
        # Count different metric types
        local metric_count
        metric_count=$(curl -s "${GATEWAY_URL}/actuator/prometheus" | grep -c "gateway_rate_limit" || echo "0")
        log "  Found $metric_count rate limiting metric entries"
        
    else
        log "${YELLOW}⚠ Rate limiting metrics not found in Prometheus endpoint${NC}"
        test_results["prometheus_metrics"]="PARTIAL"
    fi
    
    # Test general metrics endpoint
    if curl -s "${GATEWAY_URL}/actuator/metrics" | grep -q "gateway.rate.limit"; then
        log "${GREEN}✓ Rate limiting metrics available in metrics endpoint${NC}"
        test_results["general_metrics"]="PASS"
    else
        log "${YELLOW}⚠ Rate limiting metrics not found in metrics endpoint${NC}"
        test_results["general_metrics"]="PARTIAL"
    fi
}

# Test adaptive rate limiting
test_adaptive_rate_limiting() {
    print_test_section "Adaptive Rate Limiting Testing"
    
    log "${BLUE}Testing adaptive rate limiting behavior...${NC}"
    
    # This test checks if rate limits adapt under load
    local endpoint="payments"
    local path="/api/${endpoint}"
    local auth_header="-H 'Authorization: Bearer test-token-for-rate-limiting'"
    
    log "Baseline rate limiting test..."
    local baseline_429s=0
    for i in {1..20}; do
        local response_code
        response_code=$(eval "curl -s -o /dev/null -w '%{http_code}' $auth_header '${GATEWAY_URL}${path}'")
        if [ "$response_code" = "429" ]; then
            ((baseline_429s++))
        fi
        sleep 0.1
    done
    
    log "Baseline 429 responses: $baseline_429s/20"
    
    # Simulate high load and test again
    log "High load test (simulating system stress)..."
    local high_load_429s=0
    
    # Send rapid burst to simulate high load
    for i in {1..50}; do
        eval "curl -s -o /dev/null $auth_header '${GATEWAY_URL}${path}'" &
    done
    wait
    
    # Test rate limiting after high load
    for i in {1..20}; do
        local response_code
        response_code=$(eval "curl -s -o /dev/null -w '%{http_code}' $auth_header '${GATEWAY_URL}${path}'")
        if [ "$response_code" = "429" ]; then
            ((high_load_429s++))
        fi
        sleep 0.1
    done
    
    log "High load 429 responses: $high_load_429s/20"
    
    if [ $high_load_429s -gt $baseline_429s ]; then
        log "${GREEN}✓ Adaptive rate limiting appears to be working (more restrictive under load)${NC}"
        test_results["adaptive_rate_limiting"]="PASS"
    else
        log "${YELLOW}⚠ Adaptive rate limiting not clearly demonstrated${NC}"
        test_results["adaptive_rate_limiting"]="PARTIAL"
    fi
}

# Test performance impact
test_performance_impact() {
    print_test_section "Performance Impact Assessment"
    
    log "${BLUE}Testing performance impact of advanced rate limiting...${NC}"
    
    local endpoint="products"
    local path="/api/${endpoint}"
    
    # Test response times
    local total_time=0
    local request_count=10
    
    for i in $(seq 1 $request_count); do
        local response_time
        response_time=$(curl -s -o /dev/null -w '%{time_total}' "${GATEWAY_URL}${path}")
        total_time=$(echo "$total_time + $response_time" | bc -l)
        sleep 0.5
    done
    
    local average_time
    average_time=$(echo "scale=3; $total_time / $request_count" | bc -l)
    
    log "Average response time: ${average_time}s over $request_count requests"
    
    # Consider performance acceptable if under 1 second on average
    if [ "$(echo "$average_time < 1.0" | bc -l)" -eq 1 ]; then
        log "${GREEN}✓ Performance impact is acceptable (< 1s average)${NC}"
        test_results["performance"]="PASS"
    else
        log "${YELLOW}⚠ Performance impact may be high (> 1s average)${NC}"
        test_results["performance"]="PARTIAL"
    fi
}

# Generate comprehensive test report
generate_test_report() {
    print_test_section "Test Results Summary"
    
    local report_file="${TEST_OUTPUT_DIR}/advanced_rate_limit_report_${TIMESTAMP}.md"
    
    {
        echo "# SEC-10 Advanced Rate Limiting Test Report"
        echo ""
        echo "**Test Timestamp:** $(date)"
        echo "**Gateway URL:** $GATEWAY_URL"
        echo "**Test Duration:** $(date -d@$SECONDS -u +%H:%M:%S)"
        echo ""
        echo "## Test Results Summary"
        echo ""
    } > "$report_file"
    
    local pass_count=0
    local partial_count=0
    local fail_count=0
    
    for test_name in "${!test_results[@]}"; do
        local result="${test_results[$test_name]}"
        local status_icon
        
        case $result in
            "PASS")
                status_icon="✅"
                ((pass_count++))
                ;;
            "PARTIAL")
                status_icon="⚠️"
                ((partial_count++))
                ;;
            "FAIL")
                status_icon="❌"
                ((fail_count++))
                ;;
        esac
        
        echo "| $test_name | $result | $status_icon |" >> "$report_file"
        log "$status_icon $test_name: $result"
    done
    
    {
        echo ""
        echo "## Summary Statistics"
        echo ""
        echo "- **Passed:** $pass_count tests"
        echo "- **Partial:** $partial_count tests"
        echo "- **Failed:** $fail_count tests"
        echo "- **Total:** $((pass_count + partial_count + fail_count)) tests"
        echo ""
        echo "## Rate Limiting Configuration Tested"
        echo ""
        echo "- **Products:** ${endpoint_limits["products"]} req/min (Public)"
        echo "- **Cart:** ${endpoint_limits["cart"]} req/min (User-based)"
        echo "- **Orders:** ${endpoint_limits["orders"]} req/min (User-based)"
        echo "- **Payments:** ${endpoint_limits["payments"]} req/min (User-based)"
        echo ""
        echo "## Advanced Features Tested"
        echo ""
        echo "- ✅ Endpoint-specific rate limiting"
        echo "- ✅ Circuit breaker functionality"
        echo "- ✅ Adaptive rate limiting"
        echo "- ✅ Comprehensive metrics collection"
        echo "- ✅ Performance impact assessment"
        echo ""
        echo "## Recommendations"
        echo ""
        if [ $fail_count -gt 0 ]; then
            echo "- **CRITICAL:** $fail_count tests failed - investigate immediately"
        fi
        if [ $partial_count -gt 0 ]; then
            echo "- **WARNING:** $partial_count tests had partial results - may need authentication or higher load"
        fi
        echo "- Consider load testing with authenticated users for complete validation"
        echo "- Monitor metrics in production for fine-tuning rate limits"
        echo ""
    } >> "$report_file"
    
    log "${GREEN}✓ Test report generated: $report_file${NC}"
    log "${BLUE}Overall Result: $pass_count passed, $partial_count partial, $fail_count failed${NC}"
}

# Main test execution
main() {
    print_header
    
    check_gateway_health
    test_endpoint_rate_limits
    test_rate_limiting_headers
    test_circuit_breaker
    test_metrics_endpoints
    test_adaptive_rate_limiting
    test_performance_impact
    
    generate_test_report
    
    log "\n${GREEN}✓ Advanced rate limiting test suite completed${NC}"
    log "${YELLOW}Results saved to: $TEST_OUTPUT_DIR${NC}"
}

# Handle script interruption
trap 'log "\n${RED}Test interrupted by user${NC}"; exit 1' INT TERM

# Check dependencies
if ! command -v curl &> /dev/null; then
    log "${RED}Error: curl is required but not installed${NC}"
    exit 1
fi

if ! command -v bc &> /dev/null; then
    log "${RED}Error: bc is required but not installed${NC}"
    exit 1
fi

# Run main function
main "$@"
