@echo off
REM SEC-06 CORS Configuration Testing Script (Windows)
REM Tests CORS functionality across different environments and scenarios
REM Author: Prashant Sinha

setlocal enabledelayedexpansion

REM Configuration
if "%GATEWAY_URL%"=="" set GATEWAY_URL=http://localhost:8080
set TEST_RESULTS_FILE=cors-test-results-%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log
set TEST_RESULTS_FILE=%TEST_RESULTS_FILE: =0%

echo 🔒 SEC-06 CORS Configuration Testing
echo ========================================
echo Gateway URL: %GATEWAY_URL%
echo Results will be logged to: %TEST_RESULTS_FILE%
echo.

REM Initialize results file
echo CORS Testing Results - %date% %time% > "%TEST_RESULTS_FILE%"
echo Gateway URL: %GATEWAY_URL% >> "%TEST_RESULTS_FILE%"
echo ======================================== >> "%TEST_RESULTS_FILE%"

REM Test counters
set TOTAL_TESTS=0
set PASSED_TESTS=0
set FAILED_TESTS=0

echo 🧪 Starting CORS Tests...
echo.

REM Test 1: Valid local development origins
call :run_cors_test "Local Dev - Vite Server" "http://localhost:5173" "GET" "/api/products" "true"
call :run_cors_test "Local Dev - React Server" "http://localhost:3000" "GET" "/api/products" "true"
call :run_cors_test "Local Dev - Vite Preview" "http://localhost:4173" "GET" "/api/products" "true"

REM Test 2: Valid production origins
call :run_cors_test "Production - WWW Domain" "https://www.mambogo.com" "GET" "/api/products" "true"
call :run_cors_test "Production - Apex Domain" "https://mambogo.com" "GET" "/api/products" "true"

REM Test 3: Invalid origins (should fail)
call :run_cors_test "Invalid Origin - Random Domain" "https://evil.com" "GET" "/api/products" "false"
call :run_cors_test "Invalid Origin - Suspicious" "null" "GET" "/api/products" "false"

REM Test 4: Preflight requests (OPTIONS)
call :run_cors_test "Preflight - Valid Origin" "http://localhost:5173" "OPTIONS" "/api/cart" "true"
call :run_cors_test "Preflight - Invalid Origin" "https://evil.com" "OPTIONS" "/api/cart" "false"

REM Test 5: Different HTTP methods
call :run_cors_test "POST Request - Valid Origin" "http://localhost:5173" "POST" "/api/cart" "true"
call :run_cors_test "PUT Request - Valid Origin" "http://localhost:5173" "PUT" "/api/cart" "true"
call :run_cors_test "DELETE Request - Valid Origin" "http://localhost:5173" "DELETE" "/api/cart" "true"

REM Test 6: Different endpoints
call :run_cors_test "Public Endpoint" "http://localhost:5173" "GET" "/api/products" "true"
call :run_cors_test "Secured Endpoint" "http://localhost:5173" "GET" "/api/cart" "true"

echo.
echo 📊 Test Results Summary
echo =======================
echo Total Tests: !TOTAL_TESTS!
echo Passed: !PASSED_TESTS!
echo Failed: !FAILED_TESTS!
echo.

REM Write summary to file
echo. >> "%TEST_RESULTS_FILE%"
echo SUMMARY: >> "%TEST_RESULTS_FILE%"
echo Total Tests: !TOTAL_TESTS! >> "%TEST_RESULTS_FILE%"
echo Passed: !PASSED_TESTS! >> "%TEST_RESULTS_FILE%"
echo Failed: !FAILED_TESTS! >> "%TEST_RESULTS_FILE%"

REM Check results
if !FAILED_TESTS! equ 0 (
    echo 🎉 All CORS tests passed!
    echo Results saved to: %TEST_RESULTS_FILE%
    exit /b 0
) else (
    echo ❌ Some CORS tests failed!
    echo Check results in: %TEST_RESULTS_FILE%
    exit /b 1
)

:run_cors_test
set "test_name=%~1"
set "origin=%~2"
set "method=%~3"
set "endpoint=%~4"
set "should_pass=%~5"

if "%method%"=="" set method=GET
if "%endpoint%"=="" set endpoint=/api/products
if "%should_pass%"=="" set should_pass=true

set /a TOTAL_TESTS+=1

echo Testing: %test_name%
echo   Origin: %origin%
echo   Method: %method%
echo   Endpoint: %endpoint%

REM Prepare curl command
set "curl_cmd=curl -s -w "%%{http_code}" -o nul"

if "%method%"=="OPTIONS" (
    set "curl_cmd=!curl_cmd! -X OPTIONS"
    set "curl_cmd=!curl_cmd! -H "Access-Control-Request-Method: POST""
    set "curl_cmd=!curl_cmd! -H "Access-Control-Request-Headers: Authorization,Content-Type""
)

if not "%origin%"=="none" (
    set "curl_cmd=!curl_cmd! -H "Origin: %origin%""
)

set "curl_cmd=!curl_cmd! "%GATEWAY_URL%%endpoint%""

REM Execute test
for /f %%i in ('!curl_cmd! 2^>nul') do set response_code=%%i

REM Check result
set test_passed=false
if "%should_pass%"=="true" (
    if !response_code! geq 200 if !response_code! lss 400 set test_passed=true
) else (
    if !response_code! geq 400 set test_passed=true
)

REM Log result
if "!test_passed!"=="true" (
    echo   ✓ PASSED ^(HTTP !response_code!^)
    set /a PASSED_TESTS+=1
    echo PASS: %test_name% - HTTP !response_code! >> "%TEST_RESULTS_FILE%"
) else (
    echo   ✗ FAILED ^(HTTP !response_code!^)
    set /a FAILED_TESTS+=1
    echo FAIL: %test_name% - HTTP !response_code! >> "%TEST_RESULTS_FILE%"
)

echo.
goto :eof
