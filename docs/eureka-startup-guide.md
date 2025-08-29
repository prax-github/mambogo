# Eureka Service Discovery Startup Guide

## Overview

This guide provides step-by-step instructions to start and verify the Eureka service discovery setup for the Mambogo microservices architecture.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- All services built successfully

## Startup Sequence

### 1. Start Infrastructure Services

First, start the required infrastructure services using Docker Compose:

```bash
cd infra
docker-compose up -d
```

This starts:
- PostgreSQL databases (products, cart, orders, payments)
- Redis
- Kafka and Zookeeper
- Keycloak
- Zipkin

### 2. Start Config Server

```bash
cd config-server
mvn spring-boot:run
```

The Config Server will start on port 8888.

### 3. Start Eureka Server

```bash
cd backend
mvn -pl eureka-server spring-boot:run
```

The Eureka Server will start on port 8761.

### 4. Start Microservices

Start each microservice in separate terminals:

```bash
# Terminal 1 - Product Service
cd backend
mvn -pl product-service spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2 - Cart Service
cd backend
mvn -pl cart-service spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 3 - Order Service
cd backend
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4 - Payment Service
cd backend
mvn -pl payment-service spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 5 - Gateway Service
cd backend
mvn -pl gateway-service spring-boot:run -Dspring-boot.run.profiles=local
```

## Service Ports

| Service | Port | Health Check URL |
|---------|------|------------------|
| Eureka Server | 8761 | http://localhost:8761/actuator/health |
| Config Server | 8888 | http://localhost:8888/actuator/health |
| Gateway Service | 8080 | http://localhost:8080/actuator/health |
| Product Service | 8082 | http://localhost:8082/actuator/health |
| Cart Service | 8083 | http://localhost:8083/actuator/health |
| Order Service | 8084 | http://localhost:8084/actuator/health |
| Payment Service | 8085 | http://localhost:8085/actuator/health |

## Verification

### 1. Check Eureka Dashboard

Open your browser and navigate to: http://localhost:8761

You should see all services registered:
- GATEWAY-SERVICE
- PRODUCT-SERVICE
- CART-SERVICE
- ORDER-SERVICE
- PAYMENT-SERVICE

All services should show status "UP".

### 2. Run Verification Script

Use the provided verification script:

**Linux/Mac:**
```bash
chmod +x scripts/verify-eureka-setup.sh
./scripts/verify-eureka-setup.sh
```

**Windows:**
```cmd
scripts\verify-eureka-setup.bat
```

### 3. Test Gateway Routing

Test that the Gateway can route requests to all services:

```bash
# Test Product Service routing
curl http://localhost:8080/api/products/actuator/health

# Test Cart Service routing
curl http://localhost:8080/api/cart/actuator/health

# Test Order Service routing
curl http://localhost:8080/api/orders/actuator/health

# Test Payment Service routing
curl http://localhost:8080/api/payments/actuator/health
```

All should return health status "UP".

## Configuration Details

### Gateway Service Configuration

The Gateway Service is configured with:

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=1
        # ... similar routes for other services
```

### Eureka Client Configuration

All services include:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    metadata-map:
      management.port: ${server.port}
```

## Troubleshooting

### Common Issues

1. **Service Not Registering with Eureka**
   - Check that Eureka Server is running on port 8761
   - Verify the service has the Eureka client dependency
   - Check service logs for registration errors

2. **Gateway Routing Fails**
   - Ensure service discovery locator is enabled
   - Verify service names match in Eureka registry
   - Check that services are healthy in Eureka dashboard

3. **Health Checks Fail**
   - Verify actuator endpoints are exposed
   - Check database connectivity
   - Review service-specific configuration

### Logs to Monitor

- **Eureka Server**: Service registration and discovery logs
- **Gateway**: Route discovery and routing logs
- **Services**: Eureka client registration logs

### Service Discovery URLs

When services are registered with Eureka, they can be accessed via:

- **Direct**: http://localhost:8082/products (Product Service)
- **Via Gateway**: http://localhost:8080/api/products (Product Service)
- **Via Eureka**: lb://product-service (Load balanced)

## Next Steps

Once the basic setup is working:

1. **Test API Endpoints**: Use the Postman collection to test full API functionality
2. **Monitor Performance**: Use Zipkin for distributed tracing
3. **Scale Services**: Start multiple instances of services to test load balancing
4. **Security**: Configure Keycloak authentication for protected endpoints

## Support

If you encounter issues:

1. Check the service logs for error messages
2. Verify all infrastructure services are running
3. Ensure network connectivity between services
4. Review the configuration files for any typos or missing settings
