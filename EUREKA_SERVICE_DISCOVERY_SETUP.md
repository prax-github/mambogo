# Eureka Service Discovery Setup

## Overview

This document describes the implementation of Eureka service discovery for the Mambogo microservices architecture. The setup enables dynamic service discovery and load balancing across all microservices.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Gateway       │    │   Product       │    │   Cart          │
│   Service       │    │   Service       │    │   Service       │
│   (8080)        │    │   (8082)        │    │   (8083)        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Eureka        │
                    │   Server        │
                    │   (8761)        │
                    └─────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Order         │    │   Payment       │    │   Config        │
│   Service       │    │   Service       │    │   Server        │
│   (8084)        │    │   (8085)        │    │   (8888)        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Components

### 1. Eureka Server (`eureka-server`)
- **Port**: 8761
- **URL**: http://localhost:8761
- **Purpose**: Service registry and discovery server
- **Features**: 
  - Service registration
  - Health monitoring
  - Load balancing support

### 2. Microservices
All services are configured as Eureka clients:

- **Gateway Service** (8080) - API Gateway with service discovery
- **Product Service** (8082) - Product management
- **Cart Service** (8083) - Shopping cart management
- **Order Service** (8084) - Order processing
- **Payment Service** (8085) - Payment processing

## Implementation Details

### 1. Parent POM Configuration

**File**: `backend/pom.xml`

```xml
<properties>
  <java.version>17</java.version>
  <spring.boot.version>3.3.2</spring.boot.version>
  <spring.cloud.version>2023.0.3</spring.cloud.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring.boot.version}</version>
      <type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>${spring.cloud.version}</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<modules>
  <module>gateway-service</module>
  <module>product-service</module>
  <module>cart-service</module>
  <module>order-service</module>
  <module>payment-service</module>
  <module>eureka-server</module>
</modules>
```

### 2. Eureka Server Module

**File**: `backend/eureka-server/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.mambogo</groupId>
    <artifactId>backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </parent>
  <artifactId>eureka-server</artifactId>
  <name>eureka-server</name>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

**File**: `backend/eureka-server/src/main/java/com/mambogo/eureka/EurekaServerApplication.java`

```java
package com.mambogo.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
```

**File**: `backend/eureka-server/src/main/resources/application.yml`

```yaml
server:
  port: 8761

spring:
  application.name: eureka-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false

management:
  endpoints.web.exposure.include: health,info
```

### 3. Service Dependencies

All services include the Eureka client dependency:

```xml
<!-- Eureka Client -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 4. Service Configuration

Each service includes Eureka configuration in `application.yml`:

```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    metadata-map:
      management.port: ${server.port}
```

### 5. Gateway Service Discovery

**File**: `backend/gateway-service/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: gateway-service
  config:
    import: optional:configserver:http://localhost:8888
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
        - id: cart-service
          uri: lb://cart-service
          predicates:
            - Path=/api/cart/**
          filters:
            - StripPrefix=1
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**
          filters:
            - StripPrefix=1
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KC_ISSUER:http://localhost:8081/realms/ecommerce}

server:
  port: 8080

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    metadata-map:
      management.port: ${server.port}
```

## Key Features

### 1. Service Discovery
- Automatic service registration with Eureka
- Health checks and monitoring
- Service instance metadata

### 2. Load Balancing
- Client-side load balancing using Spring Cloud LoadBalancer
- Service URLs use `lb://` prefix for load balancing
- Automatic failover and retry

### 3. Gateway Integration
- Dynamic route discovery
- Service-based routing instead of hardcoded URLs
- Automatic service health monitoring

### 4. Configuration Management
- Config Server integration maintained
- Environment-specific configurations
- Centralized configuration management

## Startup Sequence

1. **Start Infrastructure** (Docker Compose):
   ```bash
   cd infra
   docker-compose up -d
   ```

2. **Start Config Server**:
   ```bash
   cd config-server
   mvn spring-boot:run
   ```

3. **Start Eureka Server**:
   ```bash
   cd backend
   mvn -pl eureka-server spring-boot:run
   ```

4. **Start Microservices**:
   ```bash
   cd backend
   mvn -pl product-service spring-boot:run -Dspring-boot.run.profiles=local
   mvn -pl cart-service spring-boot:run -Dspring-boot.run.profiles=local
   mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=local
   mvn -pl payment-service spring-boot:run -Dspring-boot.run.profiles=local
   mvn -pl gateway-service spring-boot:run -Dspring-boot.run.profiles=local
   ```

## Verification

### 1. Eureka Dashboard
- URL: http://localhost:8761
- Verify all services are registered and healthy

### 2. Service Health Checks
- Gateway: http://localhost:8080/actuator/health
- Product: http://localhost:8082/actuator/health
- Cart: http://localhost:8083/actuator/health
- Order: http://localhost:8084/actuator/health
- Payment: http://localhost:8085/actuator/health

### 3. API Testing
- Gateway routes: http://localhost:8080/api/products
- Direct service: http://localhost:8082/products

## Benefits

1. **Dynamic Service Discovery**: No hardcoded URLs
2. **Load Balancing**: Automatic distribution of requests
3. **Fault Tolerance**: Automatic failover and retry
4. **Scalability**: Easy horizontal scaling
5. **Monitoring**: Centralized service health monitoring
6. **Configuration**: Maintained Config Server integration

## Troubleshooting

### Common Issues

1. **Service Not Registering**:
   - Check Eureka server is running on port 8761
   - Verify service configuration includes Eureka client dependency
   - Check network connectivity

2. **Gateway Routes Not Working**:
   - Verify service discovery locator is enabled
   - Check service names match in Eureka registry
   - Ensure services are healthy in Eureka dashboard

3. **Load Balancing Issues**:
   - Verify multiple instances are running
   - Check service health status
   - Review load balancer configuration

### Logs to Monitor

- Eureka Server: Service registration logs
- Gateway: Route discovery and routing logs
- Services: Eureka client registration logs

## Next Steps

1. **Production Configuration**:
   - Configure Eureka server clustering
   - Add security to Eureka server
   - Configure proper health checks

2. **Monitoring**:
   - Add metrics collection
   - Configure alerts for service failures
   - Implement distributed tracing

3. **Scaling**:
   - Configure auto-scaling policies
   - Implement circuit breakers
   - Add rate limiting

---

**Last Updated**: August 29, 2025  
**Version**: 1.0  
**Status**: ✅ Implemented and Tested
