package com.mambogo.product.exception;

/**
 * General validation exception for custom validation scenarios.
 * Used when standard Bean Validation is not sufficient.
 */
public class ValidationException extends RuntimeException {

    private final String validationCode;
    private final Object invalidValue;

    public ValidationException(String message) {
        super(message);
        this.validationCode = null;
        this.invalidValue = null;
    }

    public ValidationException(String message, String validationCode) {
        super(message);
        this.validationCode = validationCode;
        this.invalidValue = null;
    }

    public ValidationException(String message, String validationCode, Object invalidValue) {
        super(message);
        this.validationCode = validationCode;
        this.invalidValue = invalidValue;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationCode = null;
        this.invalidValue = null;
    }

    public String getValidationCode() {
        return validationCode;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }
}
