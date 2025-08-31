package com.mambogo.cart.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Input sanitization utility to prevent XSS, injection attacks, and other security vulnerabilities.
 * Uses OWASP Java HTML Sanitizer for HTML content sanitization.
 */
@Component
public class InputSanitizer {

    private final PolicyFactory htmlPolicy;
    
    // Patterns for detecting potentially dangerous content
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|onclick)"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script[^>]*>.*?</script>|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=)"
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c)"
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(;|\\||&|`|\\$\\(|\\${)"
    );

    public InputSanitizer() {
        // Configure OWASP HTML Sanitizer policy
        this.htmlPolicy = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.STYLES);
    }

    /**
     * Sanitize HTML content to prevent XSS attacks
     * @param input Raw HTML input
     * @return Sanitized HTML content
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        // Use OWASP HTML Sanitizer
        String sanitized = htmlPolicy.sanitize(input);
        
        // Additional custom sanitization
        sanitized = sanitized.replaceAll("<script[^>]*>.*?</script>", "");
        sanitized = sanitized.replaceAll("javascript:", "");
        sanitized = sanitized.replaceAll("vbscript:", "");
        sanitized = sanitized.replaceAll("on\\w+\\s*=", "");
        
        return sanitized;
    }

    /**
     * Sanitize general string input for basic security
     * @param input Raw string input
     * @return Sanitized string
     */
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove null bytes and control characters
        String sanitized = input.replaceAll("\\x00", "");
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        return sanitized;
    }

    /**
     * Check if input contains SQL injection patterns
     * @param input Input to check
     * @return true if suspicious patterns detected
     */
    public boolean containsSqlInjection(String input) {
        if (input == null) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains XSS patterns
     * @param input Input to check
     * @return true if suspicious patterns detected
     */
    public boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains path traversal patterns
     * @param input Input to check
     * @return true if suspicious patterns detected
     */
    public boolean containsPathTraversal(String input) {
        if (input == null) {
            return false;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains command injection patterns
     * @param input Input to check
     * @return true if suspicious patterns detected
     */
    public boolean containsCommandInjection(String input) {
        if (input == null) {
            return false;
        }
        return COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Comprehensive security check for suspicious input
     * @param input Input to check
     * @return true if any suspicious patterns detected
     */
    public boolean isSuspiciousInput(String input) {
        return containsSqlInjection(input) || 
               containsXss(input) || 
               containsPathTraversal(input) || 
               containsCommandInjection(input);
    }

    /**
     * Get description of detected security threats
     * @param input Input to analyze
     * @return Description of threats found
     */
    public String getSecurityThreats(String input) {
        if (input == null) {
            return "No threats detected";
        }
        
        StringBuilder threats = new StringBuilder();
        
        if (containsSqlInjection(input)) {
            threats.append("SQL Injection, ");
        }
        if (containsXss(input)) {
            threats.append("XSS, ");
        }
        if (containsPathTraversal(input)) {
            threats.append("Path Traversal, ");
        }
        if (containsCommandInjection(input)) {
            threats.append("Command Injection, ");
        }
        
        if (threats.length() > 0) {
            threats.setLength(threats.length() - 2); // Remove trailing comma
            return threats.toString();
        }
        
        return "No threats detected";
    }
}
