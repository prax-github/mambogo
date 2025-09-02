# CON-04: Topic Conventions Interview Guide

**Task ID:** CON-04  
**Task Name:** Topic conventions (name/partitions/retention/keys)  
**Status:** ✅ Completed  
**Date:** 2025-08-29  
**Owner:** Prashant Sinha  

---

## 🎯 Interview Context

This guide covers the establishment of comprehensive Kafka topic naming conventions, partitioning strategies, retention policies, and key selection guidelines for the e-commerce event-driven architecture. The interviewer will assess your understanding of Kafka best practices, performance optimization, and distributed systems design.

---

## 📋 Key Topics to Master

### 1. Topic Naming Conventions

#### Standard Naming Pattern
```
{domain}.{event-type}.{version}
```

#### Domain-Specific Topics
- **Order Domain**: `order-events`, `order-created`, `order-cancelled`
- **Payment Domain**: `payment-events`, `payment-authorized`, `payment-failed`
- **Inventory Domain**: `inventory-events`, `inventory-reserved`, `inventory-released`
- **Dead Letter Queues**: `order-events.DLQ`, `payment-events.DLQ`

#### Environment-Specific Topics
- **Development**: `dev-{domain}-events`
- **Staging**: `staging-{domain}-events`
- **Production**: `{domain}-events`

### 2. Partitioning Strategies

#### Partition Count Guidelines
```yaml
# High-throughput topics (order events)
order-events: 8 partitions
payment-events: 6 partitions

# Medium-throughput topics (inventory events)
inventory-events: 4 partitions

# Low-throughput topics (analytics events)
analytics-events: 2 partitions
```

#### Key Selection Strategy
```java
// Order Events - Partition by order ID
String key = orderEvent.getOrderId();

// Payment Events - Partition by order ID
String key = paymentEvent.getOrderId();

// Inventory Events - Partition by product ID
String key = inventoryEvent.getProductId();

// User Events - Partition by user ID
String key = userEvent.getUserId();
```

### 3. Retention Policies

#### Event Retention Strategy
```yaml
# Business-critical events (orders, payments)
order-events:
  retention: 30 days
  cleanup-policy: delete
  compression: lz4

payment-events:
  retention: 90 days
  cleanup-policy: delete
  compression: lz4

# Operational events (inventory, analytics)
inventory-events:
  retention: 7 days
  cleanup-policy: delete
  compression: lz4

analytics-events:
  retention: 365 days
  cleanup-policy: delete
  compression: lz4

# Dead letter queues
*.DLQ:
  retention: 7 days
  cleanup-policy: delete
  compression: lz4
```

### 4. Topic Configuration Standards

#### Standard Topic Configuration
```yaml
default-config:
  replication-factor: 3
  min-insync-replicas: 2
  cleanup-policy: delete
  compression-type: lz4
  max-message-bytes: 1048576  # 1MB
  retention-ms: 2592000000    # 30 days
  segment-ms: 86400000        # 1 day
  segment-bytes: 1073741824   # 1GB
```

---

## 🧠 Deep Dive Questions

### 1. Topic Design Principles

**Q: Why did you choose this naming convention for topics?**
**A:** The naming convention provides several benefits:
- **Domain Clarity**: Clear identification of business domains
- **Event Type**: Specific event types for targeted processing
- **Versioning**: Support for schema versioning
- **Environment Isolation**: Separate topics per environment
- **Scalability**: Easy to scale and manage

**Q: How do you determine the optimal number of partitions for a topic?**
**A:** Partition count is determined by:
- **Throughput Requirements**: Target messages per second
- **Consumer Parallelism**: Number of consumer instances
- **Storage Requirements**: Data volume and retention
- **Network Capacity**: Available network bandwidth
- **Resource Constraints**: Available CPU and memory

### 2. Partitioning and Key Selection

**Q: How do you ensure even distribution across partitions?**
**A:** Even distribution strategies:
- **Hash-Based Partitioning**: Use hash of key for distribution
- **Key Design**: Design keys for even distribution
- **Monitoring**: Monitor partition distribution
- **Rebalancing**: Rebalance partitions if needed
- **Key Analysis**: Analyze key distribution patterns

**Q: How do you handle hot partitions?**
**A:** Hot partition mitigation:
- **Key Redesign**: Redesign keys to avoid hotspots
- **Partition Splitting**: Split hot partitions
- **Load Balancing**: Distribute load across partitions
- **Monitoring**: Monitor partition usage
- **Alerting**: Alert on hot partition detection

### 3. Retention and Cleanup

**Q: How do you determine appropriate retention periods?**
**A:** Retention period factors:
- **Business Requirements**: Legal and compliance requirements
- **Storage Costs**: Storage cost considerations
- **Recovery Needs**: Disaster recovery requirements
- **Performance Impact**: Impact on cluster performance
- **Data Value**: Business value of historical data

**Q: How do you handle data cleanup and compaction?**
**A:** Cleanup and compaction:
- **Log Compaction**: Use log compaction for key-based topics
- **Delete Policy**: Configure appropriate delete policies
- **Segment Management**: Manage log segments efficiently
- **Monitoring**: Monitor cleanup performance
- **Automation**: Automate cleanup processes

### 4. Performance and Scalability

**Q: How do you optimize topic performance?**
**A:** Performance optimization:
- **Compression**: Use appropriate compression (lz4, snappy)
- **Batch Processing**: Optimize batch sizes
- **Network Tuning**: Tune network settings
- **Monitoring**: Monitor performance metrics
- **Capacity Planning**: Plan for growth

**Q: How do you handle topic scaling?**
**A:** Scaling strategies:
- **Horizontal Scaling**: Add more brokers
- **Partition Scaling**: Increase partition counts
- **Consumer Scaling**: Scale consumer applications
- **Monitoring**: Monitor scaling metrics
- **Automation**: Automate scaling decisions

---

## 🔧 Technical Implementation Questions

### 1. Topic Creation and Management

**Q: How do you automate topic creation?**
**A:** Automation approach:
- **Infrastructure as Code**: Use Terraform or similar
- **Kafka Admin API**: Use Kafka Admin API
- **CI/CD Integration**: Integrate with CI/CD pipeline
- **Configuration Management**: Manage configurations centrally
- **Validation**: Validate topic configurations

**Q: How do you manage topic configurations across environments?**
**A:** Configuration management:
- **Environment-Specific Configs**: Different configs per environment
- **Configuration Templates**: Use templates for consistency
- **Version Control**: Track configurations in Git
- **Validation**: Validate configurations
- **Documentation**: Document configuration decisions

### 2. Monitoring and Alerting

**Q: What metrics do you monitor for topics?**
**A:** Key metrics:
- **Throughput**: Messages per second
- **Latency**: End-to-end latency
- **Consumer Lag**: Consumer group lag
- **Partition Balance**: Even distribution across partitions
- **Error Rate**: Failed message rate

**Q: How do you set up alerting for topic issues?**
**A:** Alerting strategy:
- **Threshold-Based Alerts**: Alert on metric thresholds
- **Anomaly Detection**: Detect unusual patterns
- **Business Impact**: Alert on business-relevant issues
- **Escalation**: Escalate critical issues
- **Documentation**: Document alert procedures

### 3. Security and Compliance

**Q: How do you secure Kafka topics?**
**A:** Security measures:
- **Authentication**: Use SASL or SSL authentication
- **Authorization**: Use ACLs for topic access
- **Encryption**: Encrypt data in transit and at rest
- **Audit Logging**: Log access and changes
- **Monitoring**: Monitor security events

**Q: How do you ensure compliance with data retention policies?**
**A:** Compliance strategies:
- **Policy Enforcement**: Enforce retention policies
- **Audit Trails**: Maintain audit trails
- **Documentation**: Document compliance procedures
- **Monitoring**: Monitor compliance metrics
- **Reporting**: Generate compliance reports

---

## 🎯 System Design Questions

### 1. Distributed System Design

**Q: How do you design for high availability?**
**A:** High availability design:
- **Replication**: Use appropriate replication factor
- **Multi-Datacenter**: Deploy across multiple datacenters
- **Failover**: Implement automatic failover
- **Monitoring**: Monitor availability metrics
- **Testing**: Test failure scenarios

**Q: How do you handle data consistency across partitions?**
**A:** Consistency strategies:
- **Event Ordering**: Ensure event ordering within partitions
- **Idempotency**: Implement idempotent processing
- **Causality**: Track event causality
- **Monitoring**: Monitor consistency metrics
- **Testing**: Test consistency scenarios

### 2. Performance and Optimization

**Q: How do you optimize for high throughput?**
**A:** Throughput optimization:
- **Partition Count**: Optimize partition count
- **Batch Size**: Optimize batch sizes
- **Compression**: Use efficient compression
- **Network Tuning**: Tune network settings
- **Hardware**: Use appropriate hardware

**Q: How do you handle latency requirements?**
**A:** Latency optimization:
- **Partition Strategy**: Optimize partitioning
- **Consumer Tuning**: Tune consumer settings
- **Network Optimization**: Optimize network
- **Monitoring**: Monitor latency metrics
- **SLA Management**: Manage latency SLAs

---

## 📊 Metrics and Monitoring

### 1. Performance Metrics

**Key Metrics to Track:**
- **Throughput**: Messages per second per topic
- **Latency**: End-to-end latency for messages
- **Consumer Lag**: Lag for each consumer group
- **Partition Balance**: Distribution across partitions
- **Error Rate**: Failed message rate

### 2. Operational Metrics

**Operational Indicators:**
- **Topic Count**: Number of topics
- **Partition Count**: Total partitions across cluster
- **Storage Usage**: Storage usage per topic
- **Replication Factor**: Replication factor compliance
- **Configuration Compliance**: Configuration compliance

---

## 🚀 Production Considerations

### 1. Production Deployment

**Production Requirements:**
- **High Availability**: Ensure high availability
- **Performance**: Meet performance requirements
- **Security**: Implement security measures
- **Monitoring**: Comprehensive monitoring
- **Documentation**: Complete documentation

### 2. Maintenance and Operations

**Operational Procedures:**
- **Backup and Recovery**: Backup and recovery procedures
- **Upgrade Procedures**: Safe upgrade procedures
- **Capacity Planning**: Capacity planning process
- **Incident Response**: Incident response procedures
- **Change Management**: Change management process

---

## 📝 Best Practices Summary

### 1. Topic Design Principles
- **Naming Conventions**: Use consistent naming conventions
- **Partitioning**: Optimize partitioning strategy
- **Retention**: Set appropriate retention policies
- **Configuration**: Use standard configurations
- **Documentation**: Document design decisions

### 2. Performance Guidelines
- **Throughput Optimization**: Optimize for throughput
- **Latency Management**: Manage latency requirements
- **Resource Planning**: Plan for resource requirements
- **Monitoring**: Comprehensive monitoring
- **Testing**: Test performance scenarios

### 3. Operational Standards
- **Automation**: Automate operational tasks
- **Monitoring**: Monitor operational metrics
- **Alerting**: Set up appropriate alerting
- **Documentation**: Maintain operational documentation
- **Training**: Train operational staff

---

## 🎯 Interview Success Tips

### 1. Preparation
- **Review Your Implementation**: Understand every aspect of your topic design
- **Practice Examples**: Be ready to walk through specific topic examples
- **Understand Trade-offs**: Know the pros/cons of your design decisions
- **Prepare Metrics**: Have performance metrics ready

### 2. Communication
- **Start High-Level**: Begin with topic architecture overview
- **Provide Examples**: Use concrete examples to illustrate points
- **Explain Rationale**: Justify your design decisions
- **Acknowledge Limitations**: Be honest about trade-offs and limitations

### 3. Problem-Solving
- **Think Aloud**: Explain your thought process for topic design
- **Consider Alternatives**: Discuss different design approaches
- **Ask Clarifying Questions**: Ensure you understand the requirements
- **Propose Solutions**: Offer concrete solutions to design problems

---

## 📚 Additional Resources

### Documentation
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Platform](https://docs.confluent.io/platform/current/)
- [Apache Kafka Best Practices](https://kafka.apache.org/documentation/#design)

### Tools
- [Kafka Manager](https://github.com/yahoo/kafka-manager)
- [Confluent Control Center](https://docs.confluent.io/platform/current/control-center/index.html)
- [Kafka Tool](https://www.kafkatool.com/)

### Standards
- [Kafka Protocol](https://kafka.apache.org/protocol.html)
- [Kafka Security](https://kafka.apache.org/documentation/#security)
- [Kafka Monitoring](https://kafka.apache.org/documentation/#monitoring)

---

**Remember:** The key to success is demonstrating deep understanding of Kafka best practices, performance optimization, and practical implementation experience with distributed messaging systems.
