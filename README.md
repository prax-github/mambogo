# 🛒 E-commerce Microservices MVP

**A secure, event-driven e-commerce platform built with Spring Boot 3, featuring microservices architecture, OAuth2/OIDC security, and comprehensive testing infrastructure.**

## 🚀 Project Status

**Current Phase:** Infrastructure & Foundation ✅  
**Last Updated:** 2025-09-03  
**Next Milestone:** Core Services Implementation

### ✅ **Completed Components**

#### Infrastructure & Foundation
- **Repository Structure**: Monorepo with Maven multi-module setup
- **Docker Infrastructure**: Complete Docker Compose setup with all services
- **Service Discovery**: Eureka server implementation
- **Configuration Management**: Config Server with centralized configuration
- **Security Foundation**: Keycloak OAuth2/OIDC with JWT validation
- **API Gateway**: Spring Cloud Gateway with security, rate limiting, and CORS
- **Database Infrastructure**: Complete PostgreSQL schemas for all services

#### Security Implementation
- **OAuth2/OIDC**: Complete authentication and authorization system
- **JWT Validation**: Per-service JWT validation with scope-based access control
- **Rate Limiting**: Advanced rate limiting (100 req/min per user, 1000 req/min per IP)
- **CORS Policy**: Comprehensive CORS configuration for SPA domains
- **Input Validation**: Input sanitization and validation middleware
- **Security Headers**: Content Security Policy and security headers baseline

#### Contracts & Standards
- **OpenAPI Contracts**: Version 1.0 specifications for all services
- **Error Models**: Standardized error handling with problem+json format
- **Event Schemas**: JSON Schema-based event definitions and registry
- **Topic Conventions**: Kafka topic naming and configuration standards
- **Idempotency Policy**: Request deduplication and replay prevention

#### Database Layer
- **Complete Schemas**: Production-ready schemas for Product, Order, Payment, and Inventory services
- **Performance Optimization**: Comprehensive indexing strategy with GIN indexes for JSONB
- **Business Rules**: Database-level constraints and validation functions
- **Migration Strategy**: Flyway migration scripts for version-controlled evolution
- **Testing Infrastructure**: Docker Compose testing with comprehensive validation scripts

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React SPA     │    │   Mobile App    │    │   Admin Panel   │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │   API Gateway (8080)     │
                    │  • OAuth2/OIDC Proxy    │
                    │  • Rate Limiting         │
                    │  • CORS & Security       │
                    └─────────────┬─────────────┘
                                 │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
┌─────────▼─────────┐ ┌──────────▼──────────┐ ┌──────────▼──────────┐
│  Product Service  │ │   Cart Service      │ │   Order Service     │
│  (8081)          │ │   (8082)            │ │   (8083)            │
│  • PostgreSQL    │ │   • Redis           │ │   • PostgreSQL      │
│  • Full-text     │ │   • TTL-based       │ │   • Outbox Pattern  │
│    search        │ │   • Atomic ops      │ │   • Event-driven    │
└───────────────────┘ └─────────────────────┘ └─────────────────────┘
          │                       │                       │
          └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │   Payment Service        │
                    │   (8084)                 │
                    │   • PostgreSQL           │
                    │   • Payment processing   │
                    └─────────────┬─────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │   Inventory Service       │
                    │   (8085)                 │
                    │   • PostgreSQL           │
                    │   • Stock management     │
                    └─────────────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2+ with Spring Cloud
- **Security**: Spring Security, Keycloak OAuth2/OIDC
- **Database**: PostgreSQL 15, Redis 7
- **Message Broker**: Apache Kafka with Zookeeper
- **Service Discovery**: Netflix Eureka
- **Configuration**: Spring Cloud Config Server
- **API Documentation**: OpenAPI 3.0 with Swagger UI

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Orchestration**: Kubernetes (planned)
- **Monitoring**: Zipkin for distributed tracing
- **Testing**: Comprehensive testing infrastructure with Docker

### Frontend
- **Framework**: React 18+ with Vite
- **State Management**: Redux Toolkit
- **UI Components**: Material-UI or Ant Design (TBD)
- **Authentication**: OAuth2 PKCE flow

## 🚀 Quick Start

### Prerequisites
- Docker Desktop 4.0+
- Java 17+
- Maven 3.8+
- Node.js 18+ (for frontend)

### 1. Clone Repository
```bash
git clone <repository-url>
cd mambogo
```

### 2. Start Infrastructure
```bash
cd infra
docker-compose up -d
```

### 3. Verify Services
```bash
cd scripts
quick-test.bat  # Windows
# OR
./quick-test.sh  # Linux/Mac
```

### 4. Access Services
- **Keycloak Admin**: http://localhost:8081 (admin/admin)
- **API Gateway**: http://localhost:8080
- **Eureka**: http://localhost:8761
- **Zipkin**: http://localhost:9411

## 📊 Current Implementation Status

### Phase 1: Infrastructure & Foundation (100% Complete)
- ✅ Repository & Project Structure
- ✅ Security Foundation
- ✅ Contracts & Standards
- ✅ Database Setup (DB-01 Complete)

### Phase 2: Core Services Implementation (0% Complete)
- ⏺ Product Service
- ⏺ Cart Service
- ⏺ Order Service
- ⏺ Payment Service
- ⏺ Inventory Service

### Phase 3: Integration & Testing (0% Complete)
- ⏺ Service Integration
- ⏺ End-to-End Testing
- ⏺ Performance Testing

### Phase 4: Deployment & Monitoring (0% Complete)
- ⏺ Kubernetes Deployment
- ⏺ Monitoring & Alerting
- ⏺ Production Readiness

## 🧪 Testing Infrastructure

The project includes comprehensive testing infrastructure:

- **Database Testing**: Complete schema validation with Docker Compose
- **Integration Testing**: Service-to-service communication testing
- **Security Testing**: OAuth2 flow and JWT validation testing
- **Performance Testing**: Database performance and API response time testing

### Running Tests
```bash
# Quick connection test
cd infra/scripts
quick-test.bat

# Comprehensive schema validation
test-database-schemas.bat
```

## 📚 Documentation

- **[Product Requirements Document](📄%20Product%20Requirements%20Document%20(PRD).md)**: Complete project specifications
- **[Execution Roadmap](E-commerce%20Microservices%20MVP%20—%20Execution%20Roadmap.md)**: Detailed implementation plan
- **[Implementation Logs](implementation%20log/)**: Detailed logs for each completed task
- **[Interview Guides](interview%20notes/)**: Technical deep-dive guides for each component

## 🔧 Development Guidelines

### Code Quality
- Follow Spring Boot best practices
- Implement comprehensive error handling
- Use proper logging and monitoring
- Follow microservices design principles

### Security
- Always validate JWT tokens
- Implement proper scope-based access control
- Use HTTPS in production
- Follow OWASP security guidelines

### Testing
- Write unit tests for all business logic
- Implement integration tests for service communication
- Use Docker for consistent testing environments
- Validate database schemas before deployment

## 🤝 Contributing

1. Follow the established project structure
2. Update implementation logs for all changes
3. Ensure all tests pass before submitting
4. Follow the coding standards and security guidelines

## 📞 Support & Contact

- **Project Owner**: Prashant Sinha
- **Repository**: [GitHub Repository]
- **Documentation**: See the `docs/` directory for detailed guides

## 📄 License

This project is proprietary and confidential. All rights reserved.

---

**Last Updated**: 2025-09-03  
**Version**: MVP v1.0  
**Status**: Infrastructure Complete, Core Services Pending
