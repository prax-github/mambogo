package com.mambogo.gateway.controller;

import com.mambogo.gateway.csp.CspViolationEvent;
import com.mambogo.gateway.csp.CspViolationProcessor;
import com.mambogo.gateway.metrics.CspMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.Map;

/**
 * Controller for handling CSP (Content Security Policy) violation reports.
 * Provides endpoints for receiving and processing CSP violation reports from browsers,
 * with comprehensive logging, metrics collection, and security analysis.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@RestController
@RequestMapping("/api/csp")
public class CspViolationController {

    private static final Logger logger = LoggerFactory.getLogger(CspViolationController.class);

    private final CspViolationProcessor violationProcessor;
    private final CspMetricsCollector metricsCollector;

    public CspViolationController(CspViolationProcessor violationProcessor,
                                 CspMetricsCollector metricsCollector) {
        this.violationProcessor = violationProcessor;
        this.metricsCollector = metricsCollector;
        
        logger.info("CSP Violation Controller initialized");
    }

    /**
     * Handles CSP violation reports sent by browsers.
     * This endpoint receives JSON reports when CSP policies are violated.
     * 
     * @param violationReport The CSP violation report from the browser
     * @param exchange The server web exchange for extracting request details
     * @return Response indicating successful processing
     */
    @PostMapping("/violations")
    public ResponseEntity<Map<String, Object>> handleViolationReport(
            @RequestBody Map<String, Object> violationReport,
            ServerWebExchange exchange) {
        
        try {
            String clientIP = getClientIP(exchange);
            String userAgent = getUserAgent(exchange);
            String origin = getOrigin(exchange);
            
            logger.info("Received CSP violation report from IP: {}, Origin: {}, User-Agent: {}", 
                       clientIP, origin, userAgent);
            
            // Process the violation report
            CspViolationEvent violationEvent = parseViolationReport(violationReport, clientIP, userAgent, origin);
            
            if (violationEvent != null) {
                // Process violation through the violation processor
                violationProcessor.processViolation(violationEvent);
                
                // Record metrics
                metricsCollector.recordCspViolationReport(origin, userAgent);
                metricsCollector.recordCspViolation(
                    violationEvent.getViolatedDirective(),
                    violationEvent.getBlockedUri(),
                    violationEvent.getDocumentUri(),
                    origin
                );
                
                logger.debug("Successfully processed CSP violation: {}", violationEvent);
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Violation report processed",
                    "timestamp", Instant.now().toString()
                ));
            } else {
                logger.warn("Failed to parse CSP violation report: {}", violationReport);
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid violation report format",
                    "timestamp", Instant.now().toString()
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error processing CSP violation report: {}", e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to process violation report",
                "timestamp", Instant.now().toString()
            ));
        }
    }

    /**
     * Provides a health check endpoint for CSP violation reporting.
     * 
     * @return Health status of the CSP violation reporting system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            CspMetricsCollector.CspMetricsSummary summary = metricsCollector.getMetricsSummary();
            
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", Instant.now().toString(),
                "metrics", Map.of(
                    "totalPoliciesGenerated", summary.getTotalPoliciesGenerated(),
                    "totalViolations", summary.getTotalViolations(),
                    "activeOriginsCount", summary.getActiveOriginsCount(),
                    "violatingOriginsCount", summary.getViolatingOriginsCount(),
                    "cacheHitRatio", summary.getCacheHitRatio()
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error during CSP health check: {}", e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "unhealthy",
                "timestamp", Instant.now().toString(),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Provides CSP metrics endpoint for monitoring systems.
     * 
     * @return Current CSP metrics and statistics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        try {
            CspMetricsCollector.CspMetricsSummary summary = metricsCollector.getMetricsSummary();
            
            return ResponseEntity.ok(Map.of(
                "summary", Map.of(
                    "totalPoliciesGenerated", summary.getTotalPoliciesGenerated(),
                    "totalViolations", summary.getTotalViolations(),
                    "activeOriginsCount", summary.getActiveOriginsCount(),
                    "violatingOriginsCount", summary.getViolatingOriginsCount(),
                    "cacheHits", summary.getCacheHits(),
                    "cacheMisses", summary.getCacheMisses(),
                    "cacheHitRatio", summary.getCacheHitRatio()
                ),
                "timestamp", Instant.now().toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving CSP metrics: {}", e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to retrieve metrics",
                "timestamp", Instant.now().toString()
            ));
        }
    }

    /**
     * Parses a CSP violation report from the browser into a structured violation event.
     */
    private CspViolationEvent parseViolationReport(Map<String, Object> report, 
                                                  String clientIP, String userAgent, String origin) {
        try {
            // CSP violation reports contain a "csp-report" object
            @SuppressWarnings("unchecked")
            Map<String, Object> cspReport = (Map<String, Object>) report.get("csp-report");
            
            if (cspReport == null) {
                logger.warn("CSP violation report missing 'csp-report' field");
                return null;
            }
            
            String documentUri = (String) cspReport.get("document-uri");
            String referrer = (String) cspReport.get("referrer");
            String violatedDirective = (String) cspReport.get("violated-directive");
            String effectiveDirective = (String) cspReport.get("effective-directive");
            String originalPolicy = (String) cspReport.get("original-policy");
            String blockedUri = (String) cspReport.get("blocked-uri");
            String sourceFile = (String) cspReport.get("source-file");
            Integer lineNumber = (Integer) cspReport.get("line-number");
            Integer columnNumber = (Integer) cspReport.get("column-number");
            
            return new CspViolationEvent(
                documentUri,
                referrer,
                violatedDirective,
                effectiveDirective,
                originalPolicy,
                blockedUri,
                sourceFile,
                lineNumber,
                columnNumber,
                clientIP,
                userAgent,
                origin,
                Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Error parsing CSP violation report: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts client IP from the request.
     */
    private String getClientIP(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        try {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            logger.debug("Could not extract remote address: {}", e.getMessage());
        }
        
        return "unknown";
    }

    /**
     * Extracts User-Agent from the request.
     */
    private String getUserAgent(ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Extracts Origin from the request.
     */
    private String getOrigin(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getOrigin();
        return origin != null ? origin : "unknown";
    }
}
