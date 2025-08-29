# Chaos Testing Scenarios

## DLQ (Dead Letter Queue) Testing

### Scenario: Payment Service Database Failure

This test simulates a payment service database failure to verify that failed payment events are properly routed to the DLQ.

#### Prerequisites
- All services are running
- Kafka topics are created: `order-events`, `payment-events`, `payment-events.DLQ`
- Postman collection is imported and configured

#### Test Steps

1. **Stop Payment Database**
   ```bash
   docker stop postgres-payments
   ```

2. **Create Order**
   - Use the "Create Order" request from the Postman collection
   - Set `Idempotency-Key: chaos-test-123`
   - This should succeed and create an order

3. **Verify Order Creation**
   - Use the "Get Order" request to confirm the order was created
   - Check that the order status is `PENDING`

4. **Monitor DLQ**
   - Check the `payment-events.DLQ` topic for failed payment events
   - You can use Kafka tools or monitoring to verify:
   ```bash
   # Using kafka-console-consumer (if available)
   kafka-console-consumer --bootstrap-server localhost:29092 --topic payment-events.DLQ --from-beginning
   ```

5. **Restart Payment Database**
   ```bash
   docker start postgres-payments
   ```

6. **Verify Recovery**
   - Wait for the payment service to process the DLQ messages
   - Check that the order status changes to `CONFIRMED` or `PROCESSING`

#### Expected Results

- Order creation should succeed even when payment service is down
- Payment events should be routed to `payment-events.DLQ`
- After database restart, payment processing should resume
- Order status should eventually update to reflect payment processing

#### Monitoring Points

- **Order Service Logs**: Check for outbox event publishing attempts
- **Payment Service Logs**: Check for DLQ message consumption after restart
- **Kafka Topics**: Monitor message counts on all three topics
- **Database**: Verify order status changes in the orders database

### Additional Chaos Scenarios

#### Gateway Failure
1. Stop the gateway service
2. Verify that all external requests fail
3. Restart gateway service
4. Verify service recovery

#### Eureka Service Discovery Failure
1. Stop the Eureka server
2. Verify that existing connections continue to work
3. Test new service discovery
4. Restart Eureka server

#### Kafka Failure
1. Stop Kafka broker
2. Verify that order creation still works (outbox pattern)
3. Restart Kafka
4. Verify event publishing resumes

#### Redis Failure (Cart Service)
1. Stop Redis
2. Verify cart operations fail gracefully
3. Restart Redis
4. Verify cart service recovery

### Tools for Monitoring

- **Zipkin**: Distributed tracing for request flows
- **Actuator Endpoints**: Health checks and metrics
- **Kafka Tools**: Topic monitoring and message inspection
- **Docker Logs**: Service-specific logging
- **Database Monitoring**: Connection and query monitoring

### Best Practices

1. **Always use Idempotency Keys**: Prevents duplicate processing during failures
2. **Monitor DLQ Topics**: Set up alerts for DLQ message accumulation
3. **Test Recovery Procedures**: Regularly test service restart procedures
4. **Document Failure Modes**: Maintain runbooks for common failure scenarios
5. **Use Circuit Breakers**: Implement circuit breakers for external service calls
