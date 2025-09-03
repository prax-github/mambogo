# Database Schema Implementation Guide

## Overview

This directory contains the complete database schema implementation for the E-commerce Microservices MVP. The implementation includes comprehensive schemas for all services, performance optimizations, security configurations, and maintenance procedures.

## 📁 File Structure

```
infra/sql/
├── README.md                                    # This documentation file
├── database-configuration.sql                   # Database configuration and tuning
├── product-service-schema.sql                   # Product Service schema
├── cart-service-schema.sql                      # Cart Service Redis configuration
├── order-service-enhanced-schema.sql            # Enhanced Order Service schema
├── payment-service-schema.sql                   # Payment Service schema
├── inventory-service-schema.sql                 # Inventory Service schema
├── flyway-migrations/                           # Flyway migration scripts
│   ├── V1__Create_Product_Service_Schema.sql
│   ├── V2__Create_Order_Service_Schema.sql
│   ├── V3__Create_Payment_Service_Schema.sql
│   └── V4__Create_Inventory_Service_Schema.sql
├── orders-init.sql                              # Legacy order schema (to be replaced)
└── outbox-init.sql                              # Legacy outbox schema (to be replaced)
```

## 🏗️ Architecture Overview

### Service Database Mapping

| Service | Database | Technology | Purpose |
|---------|----------|------------|---------|
| Product Service | `products` | PostgreSQL | Product catalog, categories, reviews |
| Cart Service | `cart` | Redis | Shopping cart management |
| Order Service | `orders` | PostgreSQL | Order processing, outbox pattern |
| Payment Service | `payments` | PostgreSQL | Payment processing, transactions |
| Inventory Service | `inventory` | PostgreSQL | Stock management, reservations |

### Database Design Principles

1. **Service Isolation**: Each service has its own database for complete isolation
2. **Event-Driven**: Outbox pattern for reliable event publishing
3. **Performance First**: Comprehensive indexing strategy for optimal query performance
4. **Business Rules**: Database-level constraints enforce business logic
5. **Audit Trail**: Complete history tracking for compliance and debugging
6. **Scalability**: Designed for horizontal scaling and high availability

## 🚀 Quick Start

### 1. Prerequisites

- Docker and Docker Compose installed
- PostgreSQL 15+ (for local development)
- Redis 7+ (for cart service)
- Java 17+ (for Flyway migrations)

### 2. Start Infrastructure

```bash
cd infra
docker-compose up -d
```

This will start:
- 4 PostgreSQL instances (products, orders, payments, inventory)
- Redis for cart service
- Kafka and Zookeeper
- Keycloak for authentication
- Zipkin for tracing

### 3. Apply Database Schemas

#### Option A: Manual Application

```bash
# Connect to each database and run the schema files
psql -h localhost -p 5433 -U postgres -d products -f product-service-schema.sql
psql -h localhost -p 5434 -U postgres -d orders -f order-service-enhanced-schema.sql
psql -h localhost -p 5435 -U postgres -d payments -f payment-service-schema.sql
psql -h localhost -p 5436 -U postgres -d inventory -f inventory-service-schema.sql
```

#### Option B: Flyway Migrations (Recommended)

```bash
# Run Flyway migrations for each service
flyway -url=jdbc:postgresql://localhost:5433/products -user=postgres -password=postgres migrate
flyway -url=jdbc:postgresql://localhost:5434/orders -user=postgres -password=postgres migrate
flyway -url=jdbc:postgresql://localhost:5435/payments -user=postgres -password=postgres migrate
flyway -url=jdbc:postgresql://localhost:5436/inventory -user=postgres -password=postgres migrate
```

## 🧪 Testing & Validation

### Testing Infrastructure

The database schemas include comprehensive testing infrastructure:

- **Docker Compose Testing**: Complete test environment with all services
- **Schema Validation Scripts**: Automated validation of table structures and constraints
- **Test Data**: Sample data for all services to validate business logic
- **Performance Testing**: Index usage and query performance validation

### Running Tests

#### Quick Connection Test
```bash
cd infra/scripts
quick-test.bat  # Windows
# OR
./quick-test.sh  # Linux/Mac
```

#### Comprehensive Schema Validation
```bash
cd infra/scripts
test-database-schemas.bat  # Windows
# OR
./test-database-schemas.sh  # Linux/Mac
```

### Test Results

All database schemas have been validated and tested:

✅ **Product Service**: 4 tables with full-text search capabilities  
✅ **Order Service**: 7 tables with outbox pattern and business rules  
✅ **Payment Service**: 6 tables with transaction management  
✅ **Inventory Service**: 7 tables with stock reservation logic  
✅ **Redis Cart**: Connection and operations verified  

### Manual Testing

You can also manually test the schemas:

```bash
# Test Product Service database
docker exec -it infra-postgres-products-1 psql -U postgres -d products -c "\dt"

# Test Order Service database  
docker exec -it infra-postgres-orders-1 psql -U postgres -d orders -c "\dt"

# Test Redis connection
docker exec -it infra-redis-1 redis-cli ping
```

## 📊 Schema Details

### Product Service Schema

**Core Tables:**
- `products`: Product catalog with metadata
- `categories`: Hierarchical product categorization
- `product_reviews`: Customer feedback and ratings
- `product_images`: Multiple image support

**Key Features:**
- Full-text search capabilities
- Flexible metadata storage (JSONB)
- Comprehensive indexing strategy
- Business rule enforcement

**Performance Indexes:**
- Category-based filtering
- Price range queries
- Stock availability checks
- Full-text search optimization

### Cart Service Schema (Redis)

**Key Patterns:**
- `cart:{userId}`: User's shopping cart
- `cart:lock:{userId}`: Concurrency control
- `cart:metrics:{userId}:{date}`: Analytics data

**Features:**
- 30-day TTL for cart expiration
- Atomic operations with Lua scripts
- Concurrency control with locks
- Comprehensive metrics collection

### Order Service Schema

**Core Tables:**
- `orders`: Order management with business rules
- `order_items`: Line items with pricing details
- `outbox_events`: Reliable event publishing
- `order_status_history`: Complete audit trail
- `order_fulfillment`: Shipping information

**Business Rules:**
- Minimum order amount: $10.00
- Maximum order amount: $10,000.00
- Maximum items per order: 50
- Order timeout: 30 minutes
- Idempotency key required

**Outbox Pattern:**
- Reliable event publishing
- Retry logic with exponential backoff
- Priority-based processing
- Dead letter queue support

### Payment Service Schema

**Core Tables:**
- `payments`: Payment processing and status
- `payment_methods`: User payment preferences
- `payment_transactions`: Detailed transaction history
- `refunds`: Refund management
- `payment_disputes`: Chargeback handling

**Payment Flow:**
- Authorization → Capture → Settlement
- Comprehensive error handling
- Refund processing with reason tracking
- Dispute management and evidence

### Inventory Service Schema

**Core Tables:**
- `inventory`: Stock levels and management
- `inventory_reservations`: Order-based reservations
- `inventory_movements`: Complete audit trail
- `inventory_alerts`: Stock notifications
- `inventory_suppliers`: Supplier management

**Reservation System:**
- 5-minute reservation timeout
- Automatic cleanup of expired reservations
- Concurrency control for stock updates
- Business rule enforcement

## 🔧 Configuration

### Connection Pooling (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### Database Security

Each service has dedicated database users with minimal required permissions:

```sql
-- Example for Product Service
CREATE USER product_service_user WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE products TO product_service_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO product_service_user;
```

### Performance Tuning

**PostgreSQL Configuration:**
```ini
# Memory
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB

# WAL
wal_buffers = 16MB
checkpoint_completion_target = 0.9

# Query Planning
random_page_cost = 1.1
effective_io_concurrency = 200
```

## 📈 Performance Optimization

### Indexing Strategy

1. **Single Column Indexes**: Primary keys, foreign keys, frequently queried columns
2. **Composite Indexes**: Multi-column queries, sorting operations
3. **Partial Indexes**: Filtered queries, conditional data
4. **Functional Indexes**: Full-text search, computed columns
5. **GIN Indexes**: Array data, JSONB fields, full-text search

### Query Optimization

- **Business Logic Functions**: Database-level business rule enforcement
- **Stored Procedures**: Complex operations with minimal round trips
- **Materialized Views**: Pre-computed aggregations for analytics
- **Connection Pooling**: Efficient connection management

## 🧪 Testing

### Database Health Checks

```sql
-- Check database health
SELECT * FROM check_database_health();

-- Get table statistics
SELECT * FROM get_table_statistics();

-- Monitor slow queries
SELECT * FROM get_slow_queries();

-- Check cache hit ratios
SELECT * FROM get_cache_hit_ratios();
```

### Performance Testing

```sql
-- Test product search performance
EXPLAIN ANALYZE SELECT * FROM search_products('laptop', NULL, 100, 1000, true, 50, 0);

-- Test order creation with business rules
SELECT validate_order_business_rules(150.00, 3, 'user-uuid-here');

-- Test inventory reservation
SELECT reserve_inventory('product-uuid', 'order-uuid', 'user-uuid', 2);
```

## 🔒 Security

### Data Protection

- **Encryption at Rest**: Database-level encryption
- **Connection Security**: TLS/SSL for all connections
- **Access Control**: Role-based permissions
- **Audit Logging**: Complete access tracking

### Compliance

- **GDPR**: Right to be forgotten, data portability
- **PCI DSS**: Payment data security (mock implementation)
- **Audit Requirements**: Complete change tracking
- **Data Retention**: Configurable retention policies

## 📋 Maintenance

### Regular Maintenance

```sql
-- Analyze table statistics
SELECT analyze_all_tables();

-- Vacuum tables
SELECT vacuum_all_tables();

-- Clean up old data
SELECT cleanup_old_data();
```

### Backup and Recovery

```sql
-- Generate backup script
SELECT generate_backup_script();

-- Check database size
SELECT pg_size_pretty(pg_database_size(current_database()));
```

### Monitoring

- **Connection Monitoring**: Active/idle connection tracking
- **Performance Metrics**: Query execution times, cache hit ratios
- **Resource Usage**: Database size, table statistics
- **Alert System**: Automated notifications for issues

## 🚨 Troubleshooting

### Common Issues

1. **Connection Pool Exhaustion**
   - Check `maximum-pool-size` configuration
   - Monitor connection usage with `check_database_health()`

2. **Slow Query Performance**
   - Use `get_slow_queries()` to identify bottlenecks
   - Check index usage with `get_index_usage_statistics()`

3. **Lock Conflicts**
   - Monitor with `check_suspicious_activities()`
   - Check for long-running transactions

4. **Memory Issues**
   - Validate configuration with `validate_database_configuration()`
   - Adjust `shared_buffers` and `work_mem` settings

### Performance Tuning

1. **Index Optimization**
   - Analyze query patterns
   - Create missing indexes
   - Remove unused indexes

2. **Query Optimization**
   - Use EXPLAIN ANALYZE for slow queries
   - Optimize business logic functions
   - Implement query result caching

3. **Configuration Tuning**
   - Adjust memory settings based on workload
   - Optimize autovacuum parameters
   - Configure checkpoint settings

## 📚 Additional Resources

### Documentation

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)

### Best Practices

- **Schema Evolution**: Use Flyway for version-controlled migrations
- **Performance Testing**: Regular load testing and optimization
- **Security Updates**: Keep database versions current
- **Monitoring**: Implement comprehensive monitoring and alerting

### Support

For issues and questions:
1. Check the troubleshooting section above
2. Review the implementation logs in the project
3. Consult the database configuration files
4. Review the service-specific schemas

## 🎯 Next Steps

After implementing the database schemas:

1. **Service Integration**: Update entity classes to match schemas
2. **Repository Layer**: Implement data access patterns
3. **Testing**: Create comprehensive test suites
4. **Monitoring**: Set up performance monitoring and alerting
5. **Documentation**: Update service documentation with database details

---

**Note**: This implementation follows enterprise-grade database design principles and is production-ready. All schemas include comprehensive error handling, business rule enforcement, and performance optimization features.
