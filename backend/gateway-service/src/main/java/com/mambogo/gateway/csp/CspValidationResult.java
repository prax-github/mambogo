package com.mambogo.gateway.csp;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of CSP policy validation.
 * Contains errors, warnings, and recommendations for CSP policy improvements.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
public class CspValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> recommendations = new ArrayList<>();
    private boolean valid = true;

    /**
     * Adds an error to the validation result.
     * Errors indicate critical security issues that should be fixed.
     */
    public void addError(String error) {
        errors.add(error);
        valid = false;
    }

    /**
     * Adds a warning to the validation result.
     * Warnings indicate potential security concerns that should be reviewed.
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * Adds a recommendation to the validation result.
     * Recommendations suggest improvements to enhance security.
     */
    public void addRecommendation(String recommendation) {
        recommendations.add(recommendation);
    }

    /**
     * Returns true if the CSP policy passes validation (no errors).
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns true if there are any issues (errors, warnings, or recommendations).
     */
    public boolean hasIssues() {
        return !errors.isEmpty() || !warnings.isEmpty() || !recommendations.isEmpty();
    }

    /**
     * Gets the list of validation errors.
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Gets the list of validation warnings.
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Gets the list of validation recommendations.
     */
    public List<String> getRecommendations() {
        return new ArrayList<>(recommendations);
    }

    /**
     * Returns a summary of the validation result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (valid && !hasIssues()) {
            summary.append("CSP policy validation passed with no issues.");
            return summary.toString();
        }
        
        if (!errors.isEmpty()) {
            summary.append("ERRORS (").append(errors.size()).append("): ");
            summary.append(String.join("; ", errors));
            summary.append(". ");
        }
        
        if (!warnings.isEmpty()) {
            summary.append("WARNINGS (").append(warnings.size()).append("): ");
            summary.append(String.join("; ", warnings));
            summary.append(". ");
        }
        
        if (!recommendations.isEmpty()) {
            summary.append("RECOMMENDATIONS (").append(recommendations.size()).append("): ");
            summary.append(String.join("; ", recommendations));
            summary.append(".");
        }
        
        return summary.toString().trim();
    }

    @Override
    public String toString() {
        return "CspValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors.size() +
                ", warnings=" + warnings.size() +
                ", recommendations=" + recommendations.size() +
                '}';
    }
}
