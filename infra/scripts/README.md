# Database Schema Testing Scripts

This directory contains comprehensive testing scripts to validate all database schemas for the e-commerce microservices.

## 🚀 Quick Start

### Windows Users
```batch
# Quick connection test
quick-test.bat

# Comprehensive schema validation
test-database-schemas.bat
```

### Linux/Mac Users
```bash
# Make script executable (first time only)
chmod +x test-database-schemas.sh

# Run comprehensive tests
./test-database-schemas.sh
```

## 📋 What Gets Tested

### 1. **Database Connections**
- Product Service (PostgreSQL on port 5433)
- Order Service (PostgreSQL on port 5434)
- Payment Service (PostgreSQL on port 5435)
- Inventory Service (PostgreSQL on port 5436)
- Redis (port 6379)

### 2. **Schema Validation**
- Table existence and structure
- Required columns and data types
- Foreign key relationships
- Indexes and constraints

### 3. **Data Integrity**
- Business rule enforcement
- Constraint validation
- Data type validation
- Referential integrity

### 4. **Business Logic**
- Order total calculations
- Inventory reservation logic
- Payment status validation
- Outbox pattern verification

### 5. **Performance**
- Index usage verification
- Query execution plans
- Database function validation

## 🔧 Prerequisites

- Docker Desktop installed and running
- Docker Compose available
- At least 4GB RAM available for all services

## 📁 File Structure

```
infra/scripts/
├── test-database-schemas.sh      # Linux/Mac comprehensive test
├── test-database-schemas.bat     # Windows comprehensive test
├── quick-test.bat               # Windows quick connection test
└── README.md                    # This file
```

## 🧪 Test Execution

### Quick Test (Windows)
The `quick-test.bat` script provides a fast way to verify basic connectivity:
- Starts all Docker services
- Tests database connections
- Reports success/failure status
- Keeps services running for manual testing

### Comprehensive Test
The full testing scripts (`test-database-schemas.sh` or `.bat`) perform:
- Complete schema validation
- Data integrity checks
- Business logic validation
- Performance testing
- Comprehensive reporting
- Automatic cleanup

## 📊 Test Results

Tests are categorized and color-coded:
- 🟢 **SUCCESS**: Test passed
- 🔴 **ERROR**: Test failed
- 🟡 **WARNING**: Test passed with warnings

Final summary shows:
- Total tests executed
- Number of passed tests
- Number of failed tests
- Overall success/failure status

## 🐛 Troubleshooting

### Common Issues

1. **Port Conflicts**
   - Ensure ports 5433-5436 and 6379 are available
   - Stop any existing PostgreSQL/Redis instances

2. **Docker Issues**
   - Verify Docker Desktop is running
   - Check Docker has sufficient resources (4GB+ RAM)

3. **Service Startup Delays**
   - Services may take 30-60 seconds to fully initialize
   - Check Docker logs: `docker-compose logs [service-name]`

4. **Permission Issues (Linux/Mac)**
   - Make script executable: `chmod +x test-database-schemas.sh`

### Debug Mode

To see detailed output, modify the scripts:
- Remove `>nul 2>&1` redirects in Windows scripts
- Remove `>/dev/null 2>&1` redirects in Linux scripts

## 🔄 Manual Testing

After running tests, you can manually validate:

```bash
# Connect to Product Service database
docker exec -it mambogo-postgres-products-1 psql -U postgres -d products

# Connect to Order Service database
docker exec -it mambogo-postgres-orders-1 psql -U postgres -d orders

# Connect to Payment Service database
docker exec -it mambogo-postgres-payments-1 psql -U postgres -d payments

# Connect to Inventory Service database
docker exec -it mambogo-postgres-inventory-1 psql -U postgres -d inventory

# Test Redis
docker exec -it mambogo-redis-1 redis-cli
```

## 📈 Performance Testing

The comprehensive tests include performance validation:
- Index usage verification
- Query execution plan analysis
- Database function performance
- Connection pool testing

## 🧹 Cleanup

### Automatic Cleanup
- Comprehensive tests automatically stop services
- Quick tests keep services running for manual testing

### Manual Cleanup
```bash
# Stop all services
docker-compose -f ../docker-compose.yml down

# Remove volumes (WARNING: destroys all data)
docker-compose -f ../docker-compose.yml down -v

# Remove images
docker-compose -f ../docker-compose.yml down --rmi all
```

## 🔮 Future Enhancements

Planned improvements:
- Load testing with large datasets
- Automated performance benchmarking
- Integration with CI/CD pipelines
- Custom test case creation
- Test result export (JSON/XML)

## 📞 Support

If you encounter issues:
1. Check Docker logs for service-specific errors
2. Verify all prerequisites are met
3. Review the troubleshooting section above
4. Check the main project README for updates
