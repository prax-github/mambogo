@echo off
echo =====================================================
echo QUICK DATABASE SCHEMA TEST
echo =====================================================
echo.

echo Starting Docker Compose services...
docker-compose -f ..\docker-compose.yml up -d

echo.
echo Waiting for services to be ready...
timeout /t 30 /nobreak >nul

echo.
echo Testing database connections...

REM Test Product Service
echo Testing Product Service database...
docker exec infra-postgres-products-1 pg_isready -U postgres -d products >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] Product Service database connection
) else (
    echo [ERROR] Product Service database connection failed
)

REM Test Order Service
echo Testing Order Service database...
docker exec infra-postgres-orders-1 pg_isready -U postgres -d orders >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] Order Service database connection
) else (
    echo [ERROR] Order Service database connection failed
)

REM Test Payment Service
echo Testing Payment Service database...
docker exec infra-postgres-payments-1 pg_isready -U postgres -d payments >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] Payment Service database connection
) else (
    echo [ERROR] Payment Service database connection failed
)

REM Test Inventory Service
echo Testing Inventory Service database...
docker exec infra-postgres-inventory-1 pg_isready -U postgres -d inventory >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] Inventory Service database connection
) else (
    echo [ERROR] Inventory Service database connection failed
)

REM Test Redis
echo Testing Redis...
docker exec infra-redis-1 redis-cli ping >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] Redis connection
) else (
    echo [ERROR] Redis connection failed
)

echo.
echo =====================================================
echo QUICK TEST COMPLETE
echo =====================================================
echo.
echo To run comprehensive tests, use: test-database-schemas.bat
echo To stop services, use: docker-compose -f ..\docker-compose.yml down
echo.
pause
