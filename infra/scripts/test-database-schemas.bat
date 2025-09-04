@echo off
REM =====================================================
REM DATABASE SCHEMA TESTING SCRIPT (Windows)
REM =====================================================
REM This script validates all database schemas for the e-commerce microservices
REM using Docker Compose and comprehensive test queries

setlocal enabledelayedexpansion

REM Configuration
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "DOCKER_COMPOSE_FILE=%PROJECT_DIR%\docker-compose.yml"

REM Test results tracking
set "TOTAL_TESTS=0"
set "PASSED_TESTS=0"
set "FAILED_TESTS=0"

REM Colors for output (Windows 10+ supports ANSI colors)
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

REM Logging functions
:log_info
echo %BLUE%[INFO]%NC% %~1
goto :eof

:log_success
echo %GREEN%[SUCCESS]%NC% %~1
set /a PASSED_TESTS+=1
set /a TOTAL_TESTS+=1
goto :eof

:log_warning
echo %YELLOW%[WARNING]%NC% %~1
goto :eof

:log_error
echo %RED%[ERROR]%NC% %~1
set /a FAILED_TESTS+=1
set /a TOTAL_TESTS+=1
goto :eof

:log_header
echo.
echo %BLUE%=====================================================%NC%
echo %BLUE%~1%NC%
echo %BLUE%=====================================================%NC%
echo.
goto :eof

REM Test database connection
:test_database_connection
set "service_name=%~1"
set "port=%~2"
set "database=%~3"

call :log_info "Testing connection to %service_name% database..."

docker exec "mambogo-%service_name%-1" pg_isready -U postgres -d "%database%" >nul 2>&1
if %errorlevel% equ 0 (
    call :log_success "Database connection successful: %service_name%"
    exit /b 0
) else (
    call :log_error "Database connection failed: %service_name%"
    exit /b 1
)

REM Test schema tables
:test_schema_tables
set "service_name=%~1"
set "port=%~2"
set "database=%~3"
set "expected_tables=%~4"

call :log_info "Testing schema tables for %service_name%..."

REM Count expected tables
set "expected_count=0"
for %%a in (%expected_tables:,= %) do set /a expected_count+=1

REM Get actual table count
for /f "tokens=*" %%i in ('docker exec "mambogo-%service_name%-1" psql -U postgres -d "%database%" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN (%expected_tables%);"') do set "actual_tables=%%i"

REM Remove whitespace
set "actual_tables=!actual_tables: =!"

if !actual_tables! equ !expected_count! (
    call :log_success "Schema validation passed: %service_name%"
    exit /b 0
) else (
    call :log_error "Schema validation failed: %service_name% (expected: !expected_count!, got: !actual_tables!)"
    exit /b 1
)

REM Test Redis
:test_redis
call :log_info "Testing Redis connection and operations..."

docker exec mambogo-redis-1 redis-cli ping | findstr "PONG" >nul
if %errorlevel% equ 0 (
    call :log_success "Redis connection successful"
    
    REM Test basic operations
    docker exec mambogo-redis-1 redis-cli SET "test:key" "test:value" >nul
    for /f "tokens=*" %%i in ('docker exec mambogo-redis-1 redis-cli GET "test:key"') do set "value=%%i"
    
    if "!value!"=="test:value" (
        call :log_success "Redis operations test passed"
        docker exec mambogo-redis-1 redis-cli DEL "test:key" >nul
        exit /b 0
    ) else (
        call :log_error "Redis operations test failed"
        exit /b 1
    )
) else (
    call :log_error "Redis connection failed"
    exit /b 1
)

REM Main testing function
:main
call :log_header "STARTING DATABASE SCHEMA VALIDATION"

REM Check if Docker Compose is running
docker-compose -f "%DOCKER_COMPOSE_FILE%" ps | findstr "Up" >nul
if %errorlevel% neq 0 (
    call :log_info "Starting Docker Compose services..."
    docker-compose -f "%DOCKER_COMPOSE_FILE%" up -d
    
    call :log_info "Waiting for services to be ready..."
    timeout /t 30 /nobreak >nul
)

REM Test database connections
call :log_header "TESTING DATABASE CONNECTIONS"

call :test_database_connection "postgres-products" "5433" "products"
call :test_database_connection "postgres-orders" "5434" "orders"
call :test_database_connection "postgres-payments" "5435" "payments"
call :test_database_connection "postgres-inventory" "5436" "inventory"

REM Test Redis
call :log_header "TESTING REDIS"
call :test_redis

REM Test schema tables
call :log_header "TESTING SCHEMA TABLES"

call :test_schema_tables "postgres-products" "5433" "products" "products,categories,product_reviews,product_images"
call :test_schema_tables "postgres-orders" "5434" "orders" "orders,order_items,outbox_events,order_status_history,order_notes,order_fulfillment,idempotency_keys"
call :test_schema_tables "postgres-payments" "5435" "payments" "payments,payment_methods,payment_transactions,refunds,payment_disputes,payment_analytics"
call :test_schema_tables "postgres-inventory" "5436" "inventory" "inventory,inventory_reservations,inventory_movements,inventory_alerts,inventory_suppliers,inventory_categories,inventory_analytics"

REM Run comprehensive validation queries
call :log_header "RUNNING COMPREHENSIVE VALIDATION"

call :log_info "Running validation queries on Product Service..."
docker exec mambogo-postgres-products-1 psql -U postgres -d products -f /docker-entrypoint-initdb.d/01-product-schema.sql >nul 2>&1

call :log_info "Running validation queries on Order Service..."
docker exec mambogo-postgres-orders-1 psql -U postgres -d orders -f /docker-entrypoint-initdb.d/01-order-schema.sql >nul 2>&1

call :log_info "Running validation queries on Payment Service..."
docker exec mambogo-postgres-payments-1 psql -U postgres -d payments -f /docker-entrypoint-initdb.d/01-payment-schema.sql >nul 2>&1

call :log_info "Running validation queries on Inventory Service..."
docker exec mambogo-postgres-inventory-1 psql -U postgres -d inventory -f /docker-entrypoint-initdb.d/01-inventory-schema.sql >nul 2>&1

REM Final results
call :log_header "TEST RESULTS SUMMARY"

echo %BLUE%Total Tests:%NC% %TOTAL_TESTS%
echo %GREEN%Passed:%NC% %PASSED_TESTS%
echo %RED%Failed:%NC% %FAILED_TESTS%

if %FAILED_TESTS% equ 0 (
    call :log_success "All database schema tests passed! 🎉"
    exit /b 0
) else (
    call :log_error "Some tests failed. Please review the errors above."
    exit /b 1
)

REM Cleanup function
:cleanup
call :log_info "Cleaning up test environment..."
docker-compose -f "%DOCKER_COMPOSE_FILE%" down
goto :eof

REM Run main function
call :main
if %errorlevel% neq 0 (
    call :cleanup
    exit /b 1
)
call :cleanup
exit /b 0
