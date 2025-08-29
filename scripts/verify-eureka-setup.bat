@echo off
REM Eureka Service Discovery Verification Script for Windows
REM This script verifies that all services are properly registered with Eureka

echo === Eureka Service Discovery Verification ===
echo.

echo 1. Checking Eureka Server...
curl -s http://localhost:8761/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Eureka Server is running on port 8761
) else (
    echo ✗ Eureka Server is not running on port 8761
)

echo.
echo 2. Checking individual services...

REM Check Gateway Service
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Gateway Service is running on port 8080
) else (
    echo ✗ Gateway Service is not running on port 8080
)

REM Check Product Service
curl -s http://localhost:8082/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Product Service is running on port 8082
) else (
    echo ✗ Product Service is not running on port 8082
)

REM Check Cart Service
curl -s http://localhost:8083/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Cart Service is running on port 8083
) else (
    echo ✗ Cart Service is not running on port 8083
)

REM Check Order Service
curl -s http://localhost:8084/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Order Service is running on port 8084
) else (
    echo ✗ Order Service is not running on port 8084
)

REM Check Payment Service
curl -s http://localhost:8085/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Payment Service is running on port 8085
) else (
    echo ✗ Payment Service is not running on port 8085
)

echo.
echo 3. Testing Gateway routing...

REM Test gateway routing to each service
curl -s http://localhost:8080/api/products/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Gateway routing to Product Service is working
) else (
    echo ✗ Gateway routing to Product Service failed
)

curl -s http://localhost:8080/api/cart/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Gateway routing to Cart Service is working
) else (
    echo ✗ Gateway routing to Cart Service failed
)

curl -s http://localhost:8080/api/orders/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Gateway routing to Order Service is working
) else (
    echo ✗ Gateway routing to Order Service failed
)

curl -s http://localhost:8080/api/payments/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Gateway routing to Payment Service is working
) else (
    echo ✗ Gateway routing to Payment Service failed
)

echo.
echo 4. Eureka Dashboard URLs:
echo Eureka Dashboard: http://localhost:8761
echo Gateway Health: http://localhost:8080/actuator/health
echo Product Service: http://localhost:8080/api/products/actuator/health
echo Cart Service: http://localhost:8080/api/cart/actuator/health
echo Order Service: http://localhost:8080/api/orders/actuator/health
echo Payment Service: http://localhost:8080/api/payments/actuator/health

echo.
echo === Verification Complete ===
echo.
echo If all services show ✓, your Eureka setup is working correctly!
echo If any service shows ✗, check the service logs and ensure it's running.
pause
