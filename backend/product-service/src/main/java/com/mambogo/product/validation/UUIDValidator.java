package com.mambogo.product.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;

/**
 * Validator for UUID format validation.
 */
public class UUIDValidator implements ConstraintValidator<ValidUUID, Object> {

    private boolean allowNull;

    @Override
    public void initialize(ValidUUID constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }

        if (value instanceof UUID) {
            return true;
        }

        if (value instanceof String) {
            try {
                UUID.fromString((String) value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }
}
