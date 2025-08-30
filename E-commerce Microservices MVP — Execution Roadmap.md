# E-commerce Microservices MVP — Execution Roadmap (Live)

**Owner:** Prashant Sinha  
**Project:** Secure, event-driven e-commerce MVP (Spring Boot 3, Gateway, Keycloak, Kafka, Postgres, Redis, Zipkin, React/Vite)  
**Last Updated:** 2025-08-29  
**PRD Version:** Enhanced v2.0

---

## Legend

* ✅ Done • ⏳ In Progress • ⏺ Pending • ❌ Deferred • 🔥 Critical Path

---

## 🚦 Execution Order

### Phase 1 — Infrastructure & Foundation (Week 1, Days 1-2)

#### 1.1 Repository & Project Structure
1. ✅ INF-01 Monorepo scaffold with Maven multi-module
2. ✅ INF-02 Docker Compose setup (Keycloak, Kafka/ZK, Postgres×3, Redis, Zipkin)
3. ✅ INF-03 Config Server implementation
4. ✅ INF-04 Eureka Service Discovery
5. ✅ INF-05 Ports/environment matrix configuration
6. ✅ INF-06 GitHub repository initialized & pushed

#### 1.2 Security Foundation
7. ✅ SEC-01 Keycloak realm/clients (PKCE SPA, demo user)
8. ✅ SEC-02 Gateway OIDC + JWT validation
9. ✅ SEC-03 Per-service JWT validation
10. ✅ SEC-04 Service scopes (`product:read`, `cart:manage`, `order:write`)
11. ✅ SEC-05 Rate limiting configuration (100 req/min per user, 1000 req/min per IP)
12. ✅ SEC-06 CORS configuration for SPA domain
13. ⏺ SEC-07 Input validation and sanitization
14. ⏺ SEC-08 CORS policy implementation
15. ⏺ SEC-09 Content Security Policy headers
16. ⏺ SEC-10 Rate limiting implementation
17. ⏺ SEC-11 Input sanitization middleware

#### 1.3 Database Setup
18. ⏺ DB-01 Database schema creation (Product, Order, Payment, Inventory)
19. ⏺ DB-02 Database indexes and constraints
20. ⏺ DB-03 Flyway migration scripts
21. ⏺ DB-04 Database encryption at rest
22. ⏺ DB-05 Connection pooling configuration
23. ⏺ DB-06 Database seeding with sample data
24. ⏺ DB-07 Database backup and recovery procedures
25. ⏺ DB-08 Database performance tuning

---

### Phase 2 — Core Services Implementation (Week 1, Days 3-4)

#### 2.1 Product Service
26. ⏺ PRD-01 Product entity/repository/service layer
27. ⏺ PRD-02 Product REST endpoints (`/api/catalog/products`, `/api/catalog/products/{id}`)
28. ⏺ PRD-03 Product pagination and filtering
29. ⏺ PRD-04 Product validation and business rules
30. ⏺ PRD-05 Product caching strategy
31. ⏺ PRD-06 Product controller implementation
32. ⏺ PRD-07 Product DTOs and mapping
33. ⏺ PRD-08 API versioning strategy implementation
34. ⏺ PRD-09 API documentation with Swagger UI
35. ⏺ PRD-10 API rate limiting per endpoint
36. ⏺ PRD-11 API response caching

#### 2.2 Cart Service
37. ⏺ CRT-01 Cart aggregate (items, user ownership)
38. ⏺ CRT-02 Cart REST endpoints (`/api/cart`, `/api/cart/items`)
39. ⏺ CRT-03 Cart expiration (30 days TTL)
40. ⏺ CRT-04 Cart validation (max 100 items, max 10 per item)
41. ⏺ CRT-05 Cart cleanup job for expired carts
42. ⏺ CRT-06 Cart controller implementation
43. ⏺ CRT-07 Cart DTOs and mapping
44. ⏺ CRT-08 Redis integration for cart storage
45. ⏺ CRT-09 Cart item validation against product availability

#### 2.3 Order Service
46. ⏺ ORD-01 Order aggregate + outbox publisher
47. ⏺ ORD-02 Order REST endpoints (`/api/orders`, `/api/orders/me`)
48. ✅ RSL-04 Idempotency enforcement (via outbox table)
49. ⏺ ORD-03 Order status lifecycle (PENDING → CONFIRMED → SHIPPED → DELIVERED)
50. ⏺ ORD-04 Order validation (min $10, max $10,000, max 50 items)
51. ⏺ ORD-05 Order timeout handling (30 minutes)
52. ⏺ ORD-06 Order controller implementation
53. ⏺ ORD-07 Order DTOs and mapping
54. ⏺ ORD-08 Order entity and repository
55. ⏺ ORD-09 Order status transition validation

---

### Phase 3 — Payment & Inventory Services (Week 1, Days 5-6)

#### 3.1 Payment Service
56. ⏺ PAY-01 Payment entity + state machine
57. ⏺ PAY-02 Payment REST endpoint (`/api/payments/charge`)
58. ⏺ PAY-03 Payment validation and business rules
59. ⏺ PAY-04 Mock payment processor (90% success rate)
60. ⏺ PAY-05 Payment timeout handling (30 minutes)
61. ⏺ PAY-06 Payment controller implementation
62. ⏺ PAY-07 Payment DTOs and mapping
63. ⏺ PAY-08 Payment entity and repository
64. ⏺ PAY-09 Payment retry logic

#### 3.2 Inventory Service
65. ⏺ INV-01 Inventory Service implementation
66. ⏺ INV-02 Inventory reserve/release endpoints
67. ⏺ INV-03 Inventory validation (cannot reserve more than available)
68. ⏺ INV-04 Reservation timeout (5 minutes)
69. ⏺ INV-05 Auto-release expired reservations
70. ⏺ INV-06 Reorder point alerts (10 units)
71. ⏺ INV-07 Inventory controller implementation
72. ⏺ INV-08 Inventory DTOs and mapping
73. ⏺ INV-09 Inventory entity and repository
74. ⏺ INV-10 Inventory reservation conflict resolution

---

### Phase 4 — Event-Driven Architecture (Week 2, Days 1-2)

#### 4.1 Kafka Setup & Events
75. ✅ EVT-01 Kafka topics (`order.events`, `payment.events`, DLQ)
76. ✅ EVT-02 Outbox pattern in Order Service
77. ⏺ EVT-03 Payment consumes `order.events`
78. ⏺ EVT-04 Order status updates via events
79. ⏺ EVT-05 DLQ handling and retry logic
80. ⏺ EVT-06 Event schema validation
81. ⏺ EVT-07 Event publishing implementation
82. ⏺ EVT-08 Event consuming implementation

#### 4.2 Saga Orchestration
83. ⏺ SAG-01 Order creation saga (Order → Inventory → Payment)
84. ⏺ SAG-02 Compensation logic (release inventory on payment failure)
85. ⏺ SAG-03 Saga timeout handling
86. ⏺ SAG-04 Saga state persistence
87. ⏺ SAG-05 Saga coordinator implementation

#### 4.3 Analytics & Monitoring
88. ⏺ ANL-01 Analytics consumer (persist events)
89. ⏺ ANL-02 Business metrics collection
90. ⏺ ANL-03 Real-time dashboard data
91. ⏺ ANL-04 Analytics service implementation
92. ⏺ ANL-05 Analytics database schema
93. ⏺ ANL-06 Custom business metrics
94. ⏺ ANL-07 Alert notification system
95. ⏺ ANL-08 Performance baseline establishment
96. ⏺ ANL-09 Error tracking and reporting

---

### Phase 5 — Frontend Implementation (Week 2, Days 3-4)

#### 5.1 Core UI Components
97. ⏺ FE-01 SPA shell + OIDC/PKCE login
98. ⏺ FE-02 Product list UI with pagination
99. ⏺ FE-03 Cart UI (add/remove items)
100. ⏺ FE-04 Checkout flow (JWT integration)
101. ⏺ FE-05 Order history and status
102. ⏺ FE-06 Error handling and user feedback
103. ⏺ FE-07 React application setup with Vite
104. ⏺ FE-08 Keycloak integration for authentication
105. ⏺ FE-09 API client implementation
106. ⏺ FE-10 State management (Redux/Zustand)
107. ⏺ FE-11 Progressive Web App features
108. ⏺ FE-12 Offline functionality
109. ⏺ FE-13 Push notifications
110. ⏺ FE-14 Service worker implementation

#### 5.2 User Experience
111. ⏺ UX-01 Loading states and skeleton screens
112. ⏺ UX-02 Form validation and real-time feedback
113. ⏺ UX-03 Responsive design (mobile-first)
114. ⏺ UX-04 Accessibility (WCAG 2.1 AA)
115. ⏺ UX-05 Performance optimization (Lighthouse >90)

---

### Phase 6 — Observability & Resilience (Week 2, Days 5-6)

#### 6.1 Monitoring & Tracing
116. ✅ OBS-01 Sleuth + Zipkin integration
117. ✅ OBS-02 Correlation ID propagation
118. ⏺ OBS-03 Structured JSON logging
119. ⏺ OBS-04 Health check endpoints
120. ⏺ OBS-05 Metrics collection (Prometheus)
121. ❌ OBS-06 Grafana dashboards (optional)

#### 6.2 Resilience Patterns
122. ⏺ RSL-01 Timeouts + retries (Resilience4j)
123. ⏺ RSL-02 Circuit breakers and bulkheads
124. ⏺ RSL-03 Kafka error handler + backoff
125. ⏺ RSL-04 Fallback mechanisms
126. ⏺ RSL-05 Graceful degradation

#### 6.3 Performance & Scalability
127. ⏺ PER-01 API response time optimization (<500ms)
128. ⏺ PER-02 Database query optimization
129. ⏺ PER-03 Caching strategies
130. ⏺ PER-04 Load testing (100 concurrent users)

---

### Phase 7 — Testing & Quality Assurance (Week 2, Days 7-8)

#### 7.1 Automated Testing
131. ✅ TST-01 Health & route smoke tests
132. ✅ TST-02 Postman happy path collection
133. ⏺ TST-03 Unit tests (80% coverage target)
134. ⏺ TST-04 Integration tests
135. ⏺ TST-05 Contract tests (consumer-driven)
136. ⏺ TST-06 End-to-end tests
137. ⏺ TST-07 Service layer tests
138. ⏺ TST-08 Controller layer tests
139. ⏺ TST-09 Repository layer tests
140. ⏺ TST-10 Performance testing
141. ⏺ TST-11 Security testing automation
142. ⏺ TST-12 Load testing scenarios
143. ⏺ TST-13 Chaos testing automation

#### 7.2 Chaos Engineering
144. ⏺ CHA-01 Payment DB down → DLQ verification
145. ⏺ CHA-02 Kafka broker failure
146. ⏺ CHA-03 Network latency simulation
147. ⏺ CHA-04 Service failure recovery

#### 7.3 Security Testing
148. ⏺ SEC-12 Penetration testing (OWASP ZAP)
149. ⏺ SEC-13 Authentication testing
150. ⏺ SEC-14 Input validation testing
151. ⏺ SEC-15 Rate limiting verification

---

### Phase 8 — DevOps & Operations (Week 2, Days 9-10)

#### 8.1 CI/CD Pipeline
152. ⏺ OPS-01 Minimal CI pipeline (lint/build/test/compose)
153. ⏺ OPS-02 Docker image tagging + rollback
154. ⏺ OPS-03 Database migration automation
155. ⏺ OPS-04 Blue-green deployment strategy
156. ⏺ OPS-05 Secrets management
157. ⏺ OPS-06 Infrastructure as Code (Terraform)
158. ⏺ OPS-07 Automated security scanning
159. ⏺ OPS-08 Blue-green deployment automation
160. ⏺ OPS-09 Disaster recovery procedures

#### 8.2 Documentation
161. ⏳ OPS-10 README (full setup instructions)
162. ⏳ OPS-11 ADRs (gateway/JWT/outbox/Kafka)
163. ✅ OPS-12 OpenAPI & event schemas (v1)
164. ⏺ OPS-13 API documentation
165. ⏺ OPS-14 Deployment guides
166. ⏺ OPS-15 Architecture documentation
167. ⏺ OPS-16 Troubleshooting guide

#### 8.3 Monitoring & Alerting
168. ⏺ MON-01 Alert thresholds configuration
169. ⏺ MON-02 SLOs definition and monitoring
170. ⏺ MON-03 Error rate monitoring
171. ⏺ MON-04 Performance monitoring
172. ⏺ MON-05 Business metrics dashboard

---

### Phase 9 — Business Logic & Validation (Week 2, Days 11-12)

#### 9.1 Shared Kernel
173. ⏺ BUS-01 Exception handling + error codes
174. ⏺ BUS-02 DTO ↔ Domain mapping
175. ⏺ BUS-03 Validation annotations on DTOs
176. ⏺ BUS-04 Per-service JWT checks (RBAC)
177. ⏺ BUS-05 Business rule enforcement
178. ⏺ BUS-06 Common exception classes
179. ⏺ BUS-07 Error response standardization

#### 9.2 Data Validation
180. ⏺ VAL-01 Input sanitization
181. ⏺ VAL-02 Business rule validation
182. ⏺ VAL-03 Data integrity checks
183. ⏺ VAL-04 Audit logging
184. ⏺ VAL-05 Request/response validation
185. ⏺ VAL-06 Cross-service validation

---

### Phase 10 — Demo & Interview Preparation (Week 2, Days 13-14)

#### 10.1 Demo Preparation
186. ⏺ DEMO-01 5-minute demo script
187. ⏺ DEMO-02 Talking points (trade-offs)
188. ✅ DEMO-03 C4 diagrams export pack
189. ⏺ DEMO-04 Live demo environment setup
190. ⏺ DEMO-05 Backup demo scenarios
191. ⏺ DEMO-06 Demo data setup
192. ⏺ DEMO-07 Demo flow documentation

#### 10.2 Interview Topics
193. ⏺ INT-01 OAuth2/OIDC implementation details
194. ⏺ INT-02 Microservices communication patterns
195. ⏺ INT-03 Event-driven architecture benefits
196. ⏺ INT-04 Data consistency strategies
197. ⏺ INT-05 Resilience patterns explanation
198. ⏺ INT-06 Scalability considerations
199. ⏺ INT-07 Technical deep-dive notes
200. ⏺ INT-08 Architecture decision rationale

---

## 🔥 Critical Path Dependencies

### Must Complete Before Moving Forward:
- **Phase 1**: All infrastructure must be working before service development
- **Phase 2**: Product and Cart services must be complete before Order service
- **Phase 3**: Payment and Inventory must be ready before saga orchestration
- **Phase 4**: Events must be working before frontend checkout
- **Phase 5**: Frontend must be functional before testing
- **Phase 6**: Observability must be in place before chaos testing

### Parallel Execution Opportunities:
- Security configuration (SEC-02 to SEC-11) can run parallel with service development
- Frontend development (FE-07 to FE-14) can start after basic APIs are ready
- Documentation (OPS-10 to OPS-16) can be written alongside development
- Testing can be implemented incrementally as features are completed

---

## 📊 Success Metrics

### Functional Requirements:
- [ ] User can register and login via Keycloak
- [ ] User can browse products with filtering and pagination
- [ ] User can add/remove items from cart
- [ ] User can place order with idempotency
- [ ] Order flow: inventory → payment → confirmation
- [ ] Failed orders trigger compensation logic
- [ ] Kafka events are published and consumed
- [ ] Analytics data is collected and stored

### Non-Functional Requirements:
- [ ] API response time <500ms (95th percentile)
- [ ] System uptime >99.9%
- [ ] Error rate <1%
- [ ] Security scan passes with no critical issues
- [ ] Test coverage >80%
- [ ] Documentation is complete and accurate

### Operational Requirements:
- [ ] Health checks implemented for all services
- [ ] Monitoring and alerting configured
- [ ] Logging and tracing working correctly
- [ ] Backup and recovery procedures tested
- [ ] Deployment pipeline automated
- [ ] Rollback procedures tested

---

## 🚨 Risk Mitigation

### High-Risk Items:
1. **Keycloak Integration**: Start early, test thoroughly
2. **Kafka Event Ordering**: Implement proper event sequencing
3. **Database Consistency**: Use outbox pattern and sagas
4. **Frontend-Backend Integration**: Test JWT flow early
5. **Performance Under Load**: Implement caching and optimization

### Contingency Plans:
- **Service Failure**: Implement circuit breakers and fallbacks
- **Database Issues**: Use connection pooling and retry logic
- **Kafka Problems**: Implement DLQ and manual recovery
- **Security Issues**: Regular security scans and updates

---

## 📝 Daily Checkpoints

### End of Day 1:
- [ ] Infrastructure running (Docker Compose)
- [ ] Keycloak configured and accessible
- [ ] Product service basic endpoints working
- [ ] Frontend login flow functional

### End of Day 2:
- [ ] Cart service complete
- [ ] Order service basic structure
- [ ] Database schemas created
- [ ] Basic security working

### End of Day 3:
- [ ] Payment service mock implementation
- [ ] Inventory service basic functionality
- [ ] Kafka events publishing
- [ ] Saga orchestration started

### End of Day 4:
- [ ] Complete checkout flow
- [ ] Event consumers working
- [ ] Basic observability
- [ ] Frontend checkout UI

### End of Day 5:
- [ ] Resilience patterns implemented
- [ ] Testing coverage adequate
- [ ] Documentation updated
- [ ] Demo environment ready

---

## 🎯 Final Deliverables

1. **Working Demo**: Complete e-commerce flow from login to order confirmation
2. **Code Repository**: Well-structured, documented, tested codebase
3. **Documentation**: README, ADRs, API docs, setup guides
4. **Postman Collection**: Complete API testing suite
5. **Architecture Diagrams**: C4 model exports
6. **Demo Script**: 5-minute presentation with talking points
7. **Interview Prep**: Technical deep-dive notes and explanations

---

*This roadmap ensures a systematic approach to building a production-ready e-commerce microservices MVP while maintaining focus on the core interview topics and demonstrating real-world software engineering practices.*
