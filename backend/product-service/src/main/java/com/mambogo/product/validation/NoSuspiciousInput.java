package com.mambogo.product.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation to detect and reject suspicious input patterns
 * that might indicate XSS, SQL injection, or other security threats.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SuspiciousInputValidator.class)
@Documented
public @interface NoSuspiciousInput {
    String message() default "Input contains potentially dangerous content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Allow null values (use with @NotNull if null should not be allowed)
     */
    boolean allowNull() default true;
    
    /**
     * Check for XSS patterns
     */
    boolean checkXss() default true;
    
    /**
     * Check for SQL injection patterns
     */
    boolean checkSqlInjection() default true;
    
    /**
     * Check for path traversal patterns
     */
    boolean checkPathTraversal() default true;
    
    /**
     * Check for command injection patterns
     */
    boolean checkCommandInjection() default true;
}
