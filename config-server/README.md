# Spring Cloud Config Server

This is the centralized configuration server for the Mambogo microservices architecture.

## Overview

The Config Server provides centralized configuration management for all microservices in the Mambogo ecosystem. It stores configuration files externally and serves them to microservices on demand.

## Configuration Location

All configuration files are stored in: `C:\Users\<YourWindowsUser>\mambogo-config`

## Available Configurations

- `product-service.yml` - Product service configuration
- `order-service.yml` - Order service configuration  
- `cart-service.yml` - Cart service configuration
- `payment-service.yml` - Payment service configuration
- `gateway-service.yml` - Gateway service configuration

## Running the Config Server

1. Navigate to the config-server directory:
   ```bash
   cd config-server
   ```

2. Start the server:
   ```bash
   mvn spring-boot:run
   ```

3. The server will start on `http://localhost:8888`

## Testing the Config Server

You can test the config server by accessing these URLs in your browser:

- `http://localhost:8888/product-service/default`
- `http://localhost:8888/order-service/default`
- `http://localhost:8888/cart-service/default`
- `http://localhost:8888/payment-service/default`
- `http://localhost:8888/gateway-service/default`

These endpoints will return the configuration as JSON.

## Microservices Integration

All microservices are configured to connect to the config server via:
```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
```

The `optional:` prefix ensures that microservices can start even if the config server is not available.

## Configuration Updates

To update configurations:
1. Modify the YAML files in `C:\Users\<YourWindowsUser>\mambogo-config`
2. Restart the config server
3. Microservices will pick up the new configuration on their next restart

## Health Check

The config server exposes health endpoints at:
- `http://localhost:8888/actuator/health`
- `http://localhost:8888/actuator/info`
