package com.mambogo.product.exception;

import com.mambogo.product.config.ErrorResponseConstants;
import com.mambogo.product.util.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for validation errors across the Product Service.
 * Provides consistent error response format and comprehensive error logging.
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionHandler.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    @Autowired
    private SecurityAuditLogger auditLogger;

    /**
     * Handle Bean Validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        logger.warn("Validation failed for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        // Audit log the validation failure
        auditLogger.logValidationFailure(
            "unknown", // User ID would come from JWT context
            request.getRequestURI(),
            "BEAN_VALIDATION",
            ex.getMessage(),
            getClientIpAddress(request)
        );
        
        Map<String, Object> errorResponse = createBaseErrorResponse(
            ErrorResponseConstants.VALIDATION_ERROR,
            "Request validation failed",
            request
        );
        
        // Collect field errors
        List<Map<String, Object>> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("field", fieldError.getField());
            error.put("rejectedValue", sanitizeForLogging(fieldError.getRejectedValue()));
            error.put("message", fieldError.getDefaultMessage());
            error.put("code", fieldError.getCode());
            fieldErrors.add(error);
        }
        
        // Collect global errors
        List<Map<String, Object>> globalErrors = new ArrayList<>();
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            Map<String, Object> error = new HashMap<>();
            error.put("object", globalError.getObjectName());
            error.put("message", globalError.getDefaultMessage());
            error.put("code", globalError.getCode());
            globalErrors.add(error);
        }
        
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("globalErrors", globalErrors);
        errorResponse.put("errorCount", fieldErrors.size() + globalErrors.size());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation errors from @Validated annotations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        logger.warn("Constraint violation for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createBaseErrorResponse(
            ErrorResponseConstants.VALIDATION_ERROR,
            "Constraint validation failed",
            request
        );
        
        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
            .map(this::mapConstraintViolation)
            .collect(Collectors.toList());
        
        errorResponse.put("violations", violations);
        errorResponse.put("errorCount", violations.size());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle business rule validation errors
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        
        logger.warn("Business rule violation for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createBaseErrorResponse(
            ErrorResponseConstants.BUSINESS_RULE_VIOLATION,
            ex.getMessage(),
            request
        );
        
        if (ex.getViolations() != null && !ex.getViolations().isEmpty()) {
            errorResponse.put("violations", ex.getViolations());
            errorResponse.put("errorCount", ex.getViolations().size());
        }
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handle security validation errors (suspicious input detected)
     */
    @ExceptionHandler(SecurityValidationException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityValidation(
            SecurityValidationException ex, HttpServletRequest request) {
        
        // Log security event
        securityLogger.warn("Security validation failed for request to {} from IP {}: {}", 
            request.getRequestURI(), getClientIpAddress(request), ex.getMessage());
            
        // Audit log the security threat
        auditLogger.logSecurityThreat(
            "unknown", // User ID would come from JWT context
            request.getRequestURI(),
            ex.getThreatType() != null ? ex.getThreatType() : "UNKNOWN",
            ex.getSuspiciousInput(),
            getClientIpAddress(request)
        );
        
        Map<String, Object> errorResponse = createBaseErrorResponse(
            ErrorResponseConstants.SECURITY_VALIDATION_ERROR,
            "Request contains potentially dangerous content",
            request
        );
        
        // Don't expose detailed security information to client
        errorResponse.put("securityViolation", true);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle general validation errors
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            ValidationException ex, HttpServletRequest request) {
        
        logger.warn("Validation error for request to {}: {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> errorResponse = createBaseErrorResponse(
            ErrorResponseConstants.VALIDATION_ERROR,
            ex.getMessage(),
            request
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Create base error response structure
     */
    private Map<String, Object> createBaseErrorResponse(String code, String message, HttpServletRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("method", request.getMethod());
        errorResponse.put("service", "product-service");
        errorResponse.put("traceId", getTraceId(request));
        
        return errorResponse;
    }

    /**
     * Map constraint violation to error response format
     */
    private Map<String, Object> mapConstraintViolation(ConstraintViolation<?> violation) {
        Map<String, Object> error = new HashMap<>();
        error.put("property", violation.getPropertyPath().toString());
        error.put("invalidValue", sanitizeForLogging(violation.getInvalidValue()));
        error.put("message", violation.getMessage());
        error.put("constraint", violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
        
        return error;
    }

    /**
     * Get trace ID from request headers for correlation
     */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = request.getHeader("X-Request-ID");
        }
        if (traceId == null) {
            // Generate a simple trace ID if none provided
            traceId = "trace-" + System.currentTimeMillis();
        }
        return traceId;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Sanitize sensitive data for logging
     */
    private Object sanitizeForLogging(Object value) {
        if (value == null) {
            return null;
        }
        
        String stringValue = value.toString();
        
        // Mask sensitive patterns
        stringValue = stringValue.replaceAll("(?i)(password|token|key|secret|authorization)=[^&\\s]+", "$1=***");
        stringValue = stringValue.replaceAll("(?i)(password|token|key|secret)\":\\s*\"[^\"]+\"", "$1\":\"***\"");
        
        // Truncate very long values
        if (stringValue.length() > 200) {
            stringValue = stringValue.substring(0, 200) + "...";
        }
        
        return stringValue;
    }


}
