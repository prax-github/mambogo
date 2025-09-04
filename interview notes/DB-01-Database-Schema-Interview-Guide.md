# DB-01 Database Schema Interview Guide

**Task:** Database Schema, Indexes, Constraints & Migrations  
**Status:** ✅ Complete and Verified  
**Implementation Date:** 2025-09-03  
**Complexity:** High - Enterprise-grade database design

---

## 🎯 **Interview Context & Purpose**

This task demonstrates **enterprise-level database architecture skills** and shows you can design production-ready database schemas for microservices. It's a critical foundation that interviewers will probe deeply to understand your database design philosophy and technical depth.

**Key Interview Themes:**
- Database design principles and trade-offs
- Microservices data architecture
- Performance optimization strategies
- Data consistency and integrity
- Production readiness and scalability

---

## 🏗️ **Architecture Decisions & Rationale**

### **1. Service Isolation vs. Shared Database**

**Decision:** Each service gets its own dedicated PostgreSQL database  
**Rationale:** 
- **Complete isolation** - services can evolve independently
- **Technology flexibility** - each service can optimize its database
- **Security** - service-specific users with minimal permissions
- **Scalability** - databases can be scaled independently
- **Failure isolation** - one database failure doesn't affect others

**Trade-offs:**
- ✅ **Pros:** Isolation, flexibility, security, scalability
- ❌ **Cons:** Data duplication, eventual consistency, distributed transactions complexity

**Interview Question:** *"Why not use a single shared database for all services?"*

**Answer:** *"While a shared database would simplify transactions and ensure immediate consistency, it creates a single point of failure and tight coupling between services. In microservices, we prioritize service independence and resilience over immediate consistency. We handle data consistency through eventual consistency patterns like the outbox pattern and saga orchestration."*

### **2. PostgreSQL vs. NoSQL for Each Service**

**Decision:** PostgreSQL for Product, Order, Payment, Inventory; Redis for Cart  
**Rationale:**
- **Product Service:** Complex queries, full-text search, JSONB for flexible metadata
- **Order Service:** ACID transactions, complex business rules, audit trails
- **Payment Service:** Financial data integrity, compliance requirements
- **Inventory Service:** Complex reservation logic, stock calculations
- **Cart Service:** High-performance, temporary data, atomic operations

**Interview Question:** *"Why PostgreSQL instead of MongoDB for the Product service?"*

**Answer:** *"PostgreSQL provides ACID compliance, excellent JSONB support for flexible metadata, full-text search capabilities, and robust indexing. While MongoDB offers schema flexibility, PostgreSQL gives us the best of both worlds - structured data with flexible JSONB fields, plus enterprise features like transactions, constraints, and advanced indexing strategies."*

### **3. UUID vs. Auto-increment Primary Keys**

**Decision:** UUID primary keys for all entities  
**Rationale:**
- **Distributed generation** - no coordination needed between services
- **Security** - prevents enumeration attacks
- **Sharding ready** - can distribute data across multiple databases
- **Merge scenarios** - no conflicts when combining datasets

**Trade-offs:**
- ✅ **Pros:** Distributed generation, security, sharding ready
- ❌ **Cons:** Larger storage, slightly slower joins, less human-readable

**Interview Question:** *"What are the performance implications of using UUIDs?"*

**Answer:** *"UUIDs are 16 bytes vs 8 bytes for BIGINT, so there's a storage overhead. However, with proper indexing strategies like GIN indexes for JSONB fields and composite indexes for common query patterns, the performance impact is minimal. The benefits of distributed generation and security outweigh the storage cost. We also use proper index design to ensure UUID lookups remain fast."*

---

## 🔧 **Technical Implementation Details**

### **1. Indexing Strategy**

**Comprehensive Indexing Approach:**
- **Single-column indexes** on frequently queried fields (SKU, user_id, status)
- **Composite indexes** for multi-field queries (user_id + status + created_at)
- **Partial indexes** for filtered data (active products, pending orders)
- **GIN indexes** for JSONB fields (metadata, address fields)
- **Functional indexes** for computed values (lowercase searches)

**Interview Question:** *"How did you decide which indexes to create?"*

**Answer:** *"I analyzed the business requirements and common query patterns. For products, we need fast SKU lookups and category filtering. For orders, we query by user_id and status frequently. For inventory, we need fast stock availability checks. I created indexes that support these patterns while being mindful of write performance. The GIN indexes for JSONB fields enable efficient metadata queries without schema changes."*

### **2. Business Rule Enforcement**

**Database-Level Constraints:**
- **Check constraints** for business rules (order amounts, quantities)
- **Foreign key constraints** for referential integrity
- **Unique constraints** for business uniqueness (SKU, idempotency keys)
- **Trigger-based audit trails** for complete change tracking

**Interview Question:** *"Why enforce business rules at the database level?"*

**Answer:** *"Database-level constraints provide the last line of defense for data integrity. Even if application logic has bugs, the database ensures data consistency. This is especially important in microservices where multiple services might interact with the same data. Database constraints also make the business rules explicit and documented in the schema itself."*

### **3. Outbox Pattern Implementation**

**Reliable Event Publishing:**
- **Outbox table** in Order Service for reliable event publishing
- **Status tracking** (PENDING → PROCESSED → FAILED)
- **Retry logic** with exponential backoff
- **Dead letter queue** for failed events

**Interview Question:** *"How does the outbox pattern ensure reliable event publishing?"*

**Answer:** *"The outbox pattern uses a database transaction to ensure that when we create an order, we also insert the corresponding event into the outbox table. This guarantees that if the order is created, the event will be published. A separate process then reads from the outbox and publishes to Kafka, with retry logic and dead letter queue handling for failures. This ensures exactly-once semantics and prevents event loss."*

---

## 📊 **Performance & Scalability Considerations**

### **1. Connection Pooling Strategy**

**HikariCP Configuration:**
- **Maximum pool size:** 20 connections per service
- **Connection timeout:** 30 seconds
- **Idle timeout:** 10 minutes
- **Leak detection:** 60 seconds

**Interview Question:** *"How did you size the connection pools?"*

**Answer:** *"I sized the connection pools based on the expected load and database capacity. Each service gets 20 connections, which balances resource usage with performance. The connection timeout of 30 seconds ensures quick failure detection, while the idle timeout prevents connection waste. I also configured leak detection to catch any connection leaks early."*

### **2. Query Optimization**

**Performance Strategies:**
- **Stored procedures** for complex business logic
- **Materialized views** for expensive aggregations
- **Query result caching** for frequently accessed data
- **Connection pooling** for efficient resource usage

**Interview Question:** *"What performance optimizations did you implement?"*

**Answer:** *"I implemented multiple layers of optimization. At the database level, comprehensive indexing strategies ensure fast queries. Stored procedures encapsulate complex business logic and reduce round trips. The JSONB fields with GIN indexes enable efficient metadata queries. I also designed the schemas to minimize joins and optimize for the most common query patterns."*

---

## 🔒 **Security & Compliance**

### **1. Database Security Model**

**Security Implementation:**
- **Service-specific users** with minimal required permissions
- **Role-based access control** for different operations
- **Connection encryption** (TLS/SSL)
- **Audit logging** for all database operations

**Interview Question:** *"How do you ensure database security in a microservices architecture?"*

**Answer:** *"Each service has its own database user with only the permissions it needs. For example, the product service user can only access product-related tables. I also implemented comprehensive audit logging to track all database operations. The databases are isolated from each other, so a compromise in one service doesn't affect others."*

### **2. Data Privacy & Compliance**

**Compliance Features:**
- **Audit trails** for all data changes
- **Soft delete** capabilities for data retention
- **Data encryption** at rest (configurable)
- **Access logging** for compliance audits

**Interview Question:** *"How do you handle GDPR compliance in your database design?"*

**Answer:** *"I implemented comprehensive audit trails that track all data changes, including who made the change and when. The soft delete capability allows us to mark data as deleted while maintaining it for compliance purposes. The audit logs provide a complete history of data access and modifications, which is essential for GDPR compliance and data subject rights requests."*

---

## 🧪 **Testing & Validation Strategy**

### **1. Testing Infrastructure**

**Comprehensive Testing:**
- **Docker Compose** environment for consistent testing
- **Automated validation scripts** for schema verification
- **Test data** for business logic validation
- **Performance testing** with EXPLAIN ANALYZE

**Interview Question:** *"How do you test database schemas before production?"*

**Answer:** *"I created a complete testing infrastructure using Docker Compose that spins up all the databases with their schemas. The validation scripts automatically verify table structures, constraints, indexes, and business rules. I also include test data to validate business logic and performance characteristics. This ensures the schemas work correctly before any production deployment."*

### **2. Migration Strategy**

**Flyway Migrations:**
- **Version-controlled** schema evolution
- **Rollback capabilities** for failed migrations
- **Environment-specific** configurations
- **Automated deployment** integration

**Interview Question:** *"How do you handle database schema changes in production?"*

**Answer:** *"I use Flyway for version-controlled migrations. Each schema change is a numbered migration file that can be applied incrementally. Flyway tracks which migrations have been applied and ensures they run in order. For production, I test migrations in staging first, and the migrations are designed to be backward-compatible where possible. Critical changes include rollback scripts."*

---

## 🚨 **Common Interview Challenges & Responses**

### **Challenge 1: "Why not use a single database?"**

**Response:** *"While a single database would simplify transactions, it creates a single point of failure and tight coupling between services. In microservices, we prioritize service independence and resilience. We handle data consistency through eventual consistency patterns and distributed transaction patterns like sagas. The benefits of isolation and scalability outweigh the complexity of distributed data management."*

### **Challenge 2: "How do you handle data consistency across services?"**

**Response:** *"I implement eventual consistency through the outbox pattern and saga orchestration. The outbox pattern ensures reliable event publishing, while sagas coordinate multi-service transactions with compensation logic. For example, when creating an order, we reserve inventory, process payment, and confirm the order. If any step fails, we execute compensation logic to rollback previous steps."*

### **Challenge 3: "What about performance with all these databases?"**

**Response:** *"Each database is optimized for its specific workload. The Product service database has full-text search indexes, the Order service has business rule constraints, and the Inventory service has reservation logic. Connection pooling and proper indexing ensure good performance. The separation also allows us to scale each database independently based on its specific load patterns."*

---

## 📈 **Scalability & Future Considerations**

### **1. Horizontal Scaling Strategy**

**Scaling Approach:**
- **Database sharding** by user_id or geographic region
- **Read replicas** for read-heavy workloads
- **Connection pooling** optimization for high concurrency
- **Caching layers** (Redis) for frequently accessed data

**Interview Question:** *"How would you scale this database architecture?"*

**Answer:** *"I designed the schemas with sharding in mind. The UUID primary keys and user_id foreign keys make it easy to shard by user. I can add read replicas for read-heavy services like Product catalog. The connection pooling is configured to handle increased concurrency, and I can add Redis caching layers for frequently accessed data like product information."*

### **2. Monitoring & Observability**

**Monitoring Strategy:**
- **Database health checks** with custom functions
- **Performance metrics** collection and alerting
- **Slow query detection** and optimization
- **Resource usage** monitoring and capacity planning

**Interview Question:** *"How do you monitor database performance in production?"*

**Answer:** *"I implemented custom monitoring functions that check database health, table statistics, and performance metrics. These functions provide real-time insights into database performance. I also use PostgreSQL's built-in monitoring views to track slow queries, connection usage, and resource consumption. This data feeds into our monitoring and alerting systems."*

---

## 🎯 **Key Takeaways for Interview Success**

### **1. Demonstrate Enterprise Thinking**
- Show understanding of production requirements
- Discuss trade-offs and decision rationale
- Explain scalability and security considerations

### **2. Highlight Technical Depth**
- Discuss specific indexing strategies
- Explain business rule enforcement
- Show understanding of performance implications

### **3. Emphasize Production Readiness**
- Testing and validation strategies
- Migration and deployment considerations
- Monitoring and observability

### **4. Show Business Understanding**
- How database design supports business requirements
- Trade-offs between technical and business needs
- Long-term maintainability considerations

---

## 📚 **Additional Resources & References**

### **Technical Documentation**
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Flyway Documentation](https://flywaydb.org/documentation/)

### **Best Practices**
- Database design principles for microservices
- Performance optimization strategies
- Security and compliance considerations

### **Implementation Examples**
- Complete schema files in `infra/sql/`
- Testing scripts in `infra/scripts/`
- Docker Compose configuration in `infra/docker-compose.yml`

---

**Note:** This interview guide covers the technical depth and business understanding needed to discuss the DB-01 Database Schema task confidently in technical interviews. Focus on demonstrating your understanding of trade-offs, production considerations, and long-term architectural thinking.
