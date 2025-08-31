package com.mambogo.product.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when business rule validation fails.
 * Contains detailed information about the violations for proper error handling.
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final List<Map<String, Object>> violations;
    private final String ruleCode;

    public BusinessRuleViolationException(String message) {
        super(message);
        this.violations = null;
        this.ruleCode = null;
    }

    public BusinessRuleViolationException(String message, String ruleCode) {
        super(message);
        this.violations = null;
        this.ruleCode = ruleCode;
    }

    public BusinessRuleViolationException(String message, List<Map<String, Object>> violations) {
        super(message);
        this.violations = violations;
        this.ruleCode = null;
    }

    public BusinessRuleViolationException(String message, String ruleCode, List<Map<String, Object>> violations) {
        super(message);
        this.violations = violations;
        this.ruleCode = ruleCode;
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
        this.violations = null;
        this.ruleCode = null;
    }

    public List<Map<String, Object>> getViolations() {
        return violations;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
