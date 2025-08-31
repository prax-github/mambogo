package com.mambogo.gateway.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive CORS audit logging system.
 * 
 * Provides detailed audit trail for all CORS-related activities,
 * decisions, and security events for compliance and forensic analysis.
 * 
 * @author Prashant Sinha
 * @since SEC-08 Implementation
 */
@Component
public class CorsAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(CorsAuditLogger.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("CORS_AUDIT");
    
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    
    // Audit tracking
    private final ConcurrentLinkedQueue<AuditEvent> auditQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong auditEventCounter = new AtomicLong(0);
    private final AtomicLong totalAuditEvents = new AtomicLong(0);
    
    // Configuration
    private static final int MAX_QUEUE_SIZE = 10000;
    private static final int BATCH_FLUSH_SIZE = 100;
    private static final int FLUSH_INTERVAL_SECONDS = 30;

    public CorsAuditLogger() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Start periodic audit log flushing
        startAuditLogFlushing();
        
        logger.info("CORS Audit Logger initialized with queue size: {}, flush interval: {}s", 
                   MAX_QUEUE_SIZE, FLUSH_INTERVAL_SECONDS);
    }

    /**
     * Log CORS request audit event.
     */
    public void logCorsRequest(String origin, String method, String path, String userAgent, 
                              String clientIP, String decision, String reason) {
        CorsRequestAudit audit = new CorsRequestAudit(
            generateEventId(),
            origin, method, path, userAgent, clientIP, decision, reason
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Log CORS policy change audit event.
     */
    public void logPolicyChange(String changeType, String details, String initiatedBy, 
                               String oldValue, String newValue) {
        PolicyChangeAudit audit = new PolicyChangeAudit(
            generateEventId(),
            changeType, details, initiatedBy, oldValue, newValue
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Log CORS security violation audit event.
     */
    public void logSecurityViolation(String origin, String violationType, String severity, 
                                   String details, String action) {
        SecurityViolationAudit audit = new SecurityViolationAudit(
            generateEventId(),
            origin, violationType, severity, details, action
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Log CORS configuration change audit event.
     */
    public void logConfigurationChange(String component, String property, String oldValue, 
                                     String newValue, String reason) {
        ConfigurationChangeAudit audit = new ConfigurationChangeAudit(
            generateEventId(),
            component, property, oldValue, newValue, reason
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Log CORS access decision audit event.
     */
    public void logAccessDecision(String origin, String resource, String action, String decision, 
                                String reason, String context) {
        AccessDecisionAudit audit = new AccessDecisionAudit(
            generateEventId(),
            origin, resource, action, decision, reason, context
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Log CORS incident audit event.
     */
    public void logIncident(String incidentType, String severity, String description, 
                           String affectedOrigins, String responseActions) {
        IncidentAudit audit = new IncidentAudit(
            generateEventId(),
            incidentType, severity, description, affectedOrigins, responseActions
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Log CORS compliance audit event.
     */
    public void logComplianceEvent(String checkType, String status, String findings, 
                                  String recommendations) {
        ComplianceAudit audit = new ComplianceAudit(
            generateEventId(),
            checkType, status, findings, recommendations
        );
        
        queueAuditEvent(audit);
    }

    /**
     * Get audit statistics.
     */
    public AuditStatistics getAuditStatistics() {
        return new AuditStatistics(
            totalAuditEvents.get(),
            auditQueue.size(),
            BATCH_FLUSH_SIZE,
            FLUSH_INTERVAL_SECONDS
        );
    }

    /**
     * Force flush all pending audit events.
     */
    public void flushAuditEvents() {
        flushQueuedEvents();
    }

    /**
     * Queue audit event for processing.
     */
    private void queueAuditEvent(AuditEvent event) {
        // Check queue size to prevent memory issues
        if (auditQueue.size() >= MAX_QUEUE_SIZE) {
            // Remove oldest events if queue is full
            for (int i = 0; i < BATCH_FLUSH_SIZE; i++) {
                auditQueue.poll();
            }
            logger.warn("Audit queue full, removed {} oldest events", BATCH_FLUSH_SIZE);
        }
        
        auditQueue.offer(event);
        totalAuditEvents.incrementAndGet();
        
        // Flush if batch size reached
        if (auditQueue.size() >= BATCH_FLUSH_SIZE) {
            flushQueuedEvents();
        }
    }

    /**
     * Start periodic audit log flushing.
     */
    private void startAuditLogFlushing() {
        scheduler.scheduleAtFixedRate(this::flushQueuedEvents, 
                                     FLUSH_INTERVAL_SECONDS, 
                                     FLUSH_INTERVAL_SECONDS, 
                                     TimeUnit.SECONDS);
        
        logger.debug("Started periodic audit log flushing every {} seconds", FLUSH_INTERVAL_SECONDS);
    }

    /**
     * Flush queued audit events to log.
     */
    private void flushQueuedEvents() {
        if (auditQueue.isEmpty()) {
            return;
        }
        
        int flushedCount = 0;
        
        // Process events in batches
        while (!auditQueue.isEmpty() && flushedCount < BATCH_FLUSH_SIZE) {
            AuditEvent event = auditQueue.poll();
            if (event != null) {
                writeAuditEvent(event);
                flushedCount++;
            }
        }
        
        if (flushedCount > 0) {
            logger.debug("Flushed {} audit events to log", flushedCount);
        }
    }

    /**
     * Write audit event to structured log.
     */
    private void writeAuditEvent(AuditEvent event) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(event);
            auditLogger.info(jsonEvent);
        } catch (Exception e) {
            logger.error("Failed to serialize audit event: {}", event, e);
            // Fallback to simple logging
            auditLogger.error("AUDIT_SERIALIZATION_ERROR: {}", event.toString());
        }
    }

    /**
     * Generate unique event ID.
     */
    private String generateEventId() {
        return "CORS-AUDIT-" + System.currentTimeMillis() + "-" + auditEventCounter.incrementAndGet();
    }

    /**
     * Base audit event class.
     */
    public abstract static class AuditEvent {
        private final String eventId;
        private final String eventType;
        private final Instant timestamp;

        protected AuditEvent(String eventId, String eventType) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.timestamp = Instant.now();
        }

        // Getters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public Instant getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("AuditEvent{id='%s', type='%s', timestamp=%s}", 
                               eventId, eventType, timestamp);
        }
    }

    /**
     * CORS request audit event.
     */
    public static class CorsRequestAudit extends AuditEvent {
        private final String origin;
        private final String method;
        private final String path;
        private final String userAgent;
        private final String clientIP;
        private final String decision;
        private final String reason;

        public CorsRequestAudit(String eventId, String origin, String method, String path,
                               String userAgent, String clientIP, String decision, String reason) {
            super(eventId, "CORS_REQUEST");
            this.origin = origin;
            this.method = method;
            this.path = path;
            this.userAgent = userAgent;
            this.clientIP = clientIP;
            this.decision = decision;
            this.reason = reason;
        }

        // Getters
        public String getOrigin() { return origin; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getUserAgent() { return userAgent; }
        public String getClientIP() { return clientIP; }
        public String getDecision() { return decision; }
        public String getReason() { return reason; }
    }

    /**
     * Policy change audit event.
     */
    public static class PolicyChangeAudit extends AuditEvent {
        private final String changeType;
        private final String details;
        private final String initiatedBy;
        private final String oldValue;
        private final String newValue;

        public PolicyChangeAudit(String eventId, String changeType, String details,
                                String initiatedBy, String oldValue, String newValue) {
            super(eventId, "POLICY_CHANGE");
            this.changeType = changeType;
            this.details = details;
            this.initiatedBy = initiatedBy;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        // Getters
        public String getChangeType() { return changeType; }
        public String getDetails() { return details; }
        public String getInitiatedBy() { return initiatedBy; }
        public String getOldValue() { return oldValue; }
        public String getNewValue() { return newValue; }
    }

    /**
     * Security violation audit event.
     */
    public static class SecurityViolationAudit extends AuditEvent {
        private final String origin;
        private final String violationType;
        private final String severity;
        private final String details;
        private final String action;

        public SecurityViolationAudit(String eventId, String origin, String violationType,
                                    String severity, String details, String action) {
            super(eventId, "SECURITY_VIOLATION");
            this.origin = origin;
            this.violationType = violationType;
            this.severity = severity;
            this.details = details;
            this.action = action;
        }

        // Getters
        public String getOrigin() { return origin; }
        public String getViolationType() { return violationType; }
        public String getSeverity() { return severity; }
        public String getDetails() { return details; }
        public String getAction() { return action; }
    }

    /**
     * Configuration change audit event.
     */
    public static class ConfigurationChangeAudit extends AuditEvent {
        private final String component;
        private final String property;
        private final String oldValue;
        private final String newValue;
        private final String reason;

        public ConfigurationChangeAudit(String eventId, String component, String property,
                                       String oldValue, String newValue, String reason) {
            super(eventId, "CONFIGURATION_CHANGE");
            this.component = component;
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.reason = reason;
        }

        // Getters
        public String getComponent() { return component; }
        public String getProperty() { return property; }
        public String getOldValue() { return oldValue; }
        public String getNewValue() { return newValue; }
        public String getReason() { return reason; }
    }

    /**
     * Access decision audit event.
     */
    public static class AccessDecisionAudit extends AuditEvent {
        private final String origin;
        private final String resource;
        private final String action;
        private final String decision;
        private final String reason;
        private final String context;

        public AccessDecisionAudit(String eventId, String origin, String resource, String action,
                                  String decision, String reason, String context) {
            super(eventId, "ACCESS_DECISION");
            this.origin = origin;
            this.resource = resource;
            this.action = action;
            this.decision = decision;
            this.reason = reason;
            this.context = context;
        }

        // Getters
        public String getOrigin() { return origin; }
        public String getResource() { return resource; }
        public String getAction() { return action; }
        public String getDecision() { return decision; }
        public String getReason() { return reason; }
        public String getContext() { return context; }
    }

    /**
     * Incident audit event.
     */
    public static class IncidentAudit extends AuditEvent {
        private final String incidentType;
        private final String severity;
        private final String description;
        private final String affectedOrigins;
        private final String responseActions;

        public IncidentAudit(String eventId, String incidentType, String severity,
                           String description, String affectedOrigins, String responseActions) {
            super(eventId, "INCIDENT");
            this.incidentType = incidentType;
            this.severity = severity;
            this.description = description;
            this.affectedOrigins = affectedOrigins;
            this.responseActions = responseActions;
        }

        // Getters
        public String getIncidentType() { return incidentType; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getAffectedOrigins() { return affectedOrigins; }
        public String getResponseActions() { return responseActions; }
    }

    /**
     * Compliance audit event.
     */
    public static class ComplianceAudit extends AuditEvent {
        private final String checkType;
        private final String status;
        private final String findings;
        private final String recommendations;

        public ComplianceAudit(String eventId, String checkType, String status,
                              String findings, String recommendations) {
            super(eventId, "COMPLIANCE");
            this.checkType = checkType;
            this.status = status;
            this.findings = findings;
            this.recommendations = recommendations;
        }

        // Getters
        public String getCheckType() { return checkType; }
        public String getStatus() { return status; }
        public String getFindings() { return findings; }
        public String getRecommendations() { return recommendations; }
    }

    /**
     * Audit statistics summary.
     */
    public static class AuditStatistics {
        private final long totalEvents;
        private final int queuedEvents;
        private final int batchSize;
        private final int flushInterval;

        public AuditStatistics(long totalEvents, int queuedEvents, int batchSize, int flushInterval) {
            this.totalEvents = totalEvents;
            this.queuedEvents = queuedEvents;
            this.batchSize = batchSize;
            this.flushInterval = flushInterval;
        }

        // Getters
        public long getTotalEvents() { return totalEvents; }
        public int getQueuedEvents() { return queuedEvents; }
        public int getBatchSize() { return batchSize; }
        public int getFlushInterval() { return flushInterval; }
    }
}
