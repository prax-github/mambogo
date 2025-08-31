package com.mambogo.product.validation;

import com.mambogo.product.util.InputSanitizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validator for detecting suspicious input patterns that might indicate security threats.
 */
public class SuspiciousInputValidator implements ConstraintValidator<NoSuspiciousInput, String> {

    @Autowired
    private InputSanitizer inputSanitizer;

    private boolean allowNull;
    private boolean checkXss;
    private boolean checkSqlInjection;
    private boolean checkPathTraversal;
    private boolean checkCommandInjection;

    @Override
    public void initialize(NoSuspiciousInput constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.checkXss = constraintAnnotation.checkXss();
        this.checkSqlInjection = constraintAnnotation.checkSqlInjection();
        this.checkPathTraversal = constraintAnnotation.checkPathTraversal();
        this.checkCommandInjection = constraintAnnotation.checkCommandInjection();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        boolean isValid = true;
        StringBuilder violationMessage = new StringBuilder();

        if (checkXss && inputSanitizer.containsXss(value)) {
            isValid = false;
            violationMessage.append("XSS patterns detected. ");
        }

        if (checkSqlInjection && inputSanitizer.containsSqlInjection(value)) {
            isValid = false;
            violationMessage.append("SQL injection patterns detected. ");
        }

        if (checkPathTraversal && inputSanitizer.containsPathTraversal(value)) {
            isValid = false;
            violationMessage.append("Path traversal patterns detected. ");
        }

        if (checkCommandInjection && inputSanitizer.containsCommandInjection(value)) {
            isValid = false;
            violationMessage.append("Command injection patterns detected. ");
        }

        if (!isValid) {
            // Customize the violation message
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(violationMessage.toString().trim())
                   .addConstraintViolation();
        }

        return isValid;
    }
}
