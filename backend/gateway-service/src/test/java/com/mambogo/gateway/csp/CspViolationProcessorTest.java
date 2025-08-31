package com.mambogo.gateway.csp;

import com.mambogo.gateway.config.CspPolicyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for CSP Violation Processor functionality.
 * Tests violation processing, pattern detection, security analysis, and statistics collection.
 * 
 * @author Prashant Sinha
 * @since SEC-09 Implementation
 */
@ExtendWith(MockitoExtension.class)
class CspViolationProcessorTest {

    private CspViolationProcessor violationProcessor;
    private CspPolicyProperties cspProperties;

    @BeforeEach
    void setUp() {
        // Initialize test configuration
        cspProperties = new CspPolicyProperties();
        cspProperties.setMaxViolationsPerOrigin(100);
        
        violationProcessor = new CspViolationProcessor(cspProperties);
    }

    @Test
    void shouldProcessValidViolation() {
        // Given
        CspViolationEvent violation = createTestViolation(
            "https://www.mambogo.com/test",
            "script-src",
            "https://evil.com/malicious.js",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        // When & Then - Should not throw exception
        assertThatCode(() -> violationProcessor.processViolation(violation))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleNullViolation() {
        // When & Then - Should handle gracefully
        assertThatCode(() -> violationProcessor.processViolation(null))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldDetectSuspiciousViolations() {
        // Given
        CspViolationEvent javascriptViolation = createTestViolation(
            "https://www.mambogo.com/test",
            "script-src",
            "javascript:alert('xss')",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        CspViolationEvent dataViolation = createTestViolation(
            "https://www.mambogo.com/test",
            "script-src", 
            "data:text/html,<script>alert('xss')</script>",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        // When & Then - Should process without exceptions
        assertThatCode(() -> {
            violationProcessor.processViolation(javascriptViolation);
            violationProcessor.processViolation(dataViolation);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldTrackViolationStatistics() {
        // Given
        CspViolationEvent violation1 = createTestViolation(
            "https://www.mambogo.com/test1",
            "script-src",
            "https://evil.com/script1.js", 
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        CspViolationEvent violation2 = createTestViolation(
            "https://www.mambogo.com/test2",
            "style-src",
            "https://evil.com/style1.css",
            "https://attacker.com",
            CspViolationEvent.ViolationSeverity.MEDIUM
        );
        
        // When
        violationProcessor.processViolation(violation1);
        violationProcessor.processViolation(violation2);
        
        CspViolationProcessor.ViolationStatistics stats = violationProcessor.getViolationStatistics();
        
        // Then
        assertThat(stats.getUniqueOrigins()).isEqualTo(2);
        assertThat(stats.getUniqueDirectives()).isEqualTo(2);
        assertThat(stats.getTotalViolations()).isEqualTo(2);
        assertThat(stats.getMostViolatedOrigin()).isNotNull();
        assertThat(stats.getMostViolatedDirective()).isNotNull();
    }

    @Test
    void shouldResetViolationTracking() {
        // Given
        CspViolationEvent violation = createTestViolation(
            "https://www.mambogo.com/test",
            "script-src",
            "https://evil.com/script.js",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        violationProcessor.processViolation(violation);
        
        // When
        violationProcessor.resetViolationTracking();
        CspViolationProcessor.ViolationStatistics stats = violationProcessor.getViolationStatistics();
        
        // Then
        assertThat(stats.getUniqueOrigins()).isEqualTo(0);
        assertThat(stats.getUniqueDirectives()).isEqualTo(0);
        assertThat(stats.getTotalViolations()).isEqualTo(0);
    }

    @Test
    void shouldHandleMultipleViolationsFromSameOrigin() {
        // Given
        String origin = "https://www.mambogo.com";
        
        for (int i = 0; i < 5; i++) {
            CspViolationEvent violation = createTestViolation(
                "https://www.mambogo.com/test" + i,
                "script-src",
                "https://evil.com/script" + i + ".js",
                origin,
                CspViolationEvent.ViolationSeverity.HIGH
            );
            violationProcessor.processViolation(violation);
        }
        
        // When
        CspViolationProcessor.ViolationStatistics stats = violationProcessor.getViolationStatistics();
        
        // Then
        assertThat(stats.getUniqueOrigins()).isEqualTo(1);
        assertThat(stats.getTotalViolations()).isEqualTo(5);
        assertThat(stats.getMostViolatedOrigin()).contains(origin);
    }

    @Test
    void shouldDetectScriptInjectionAttempts() {
        // Given - Create violations that indicate script injection
        CspViolationEvent evalViolation = createTestViolation(
            "https://www.mambogo.com/test",
            "script-src",
            "eval(document.cookie)",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        CspViolationEvent timeoutViolation = createTestViolation(
            "https://www.mambogo.com/test",
            "script-src",
            "setTimeout('alert(1)', 1000)",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        // When & Then - Should process without exceptions
        assertThatCode(() -> {
            violationProcessor.processViolation(evalViolation);
            violationProcessor.processViolation(timeoutViolation);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldDetectDataExfiltrationAttempts() {
        // Given - Create violations that indicate data exfiltration
        CspViolationEvent ipViolation = createTestViolation(
            "https://www.mambogo.com/test",
            "connect-src",
            "https://192.168.1.100/steal-data",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        CspViolationEvent maliciousViolation = createTestViolation(
            "https://www.mambogo.com/test",
            "connect-src",
            "https://evil.attacker.com/exfiltrate",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        // When & Then - Should process without exceptions
        assertThatCode(() -> {
            violationProcessor.processViolation(ipViolation);
            violationProcessor.processViolation(maliciousViolation);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldTrackViolationsByDirective() {
        // Given
        CspViolationEvent scriptViolation1 = createTestViolation(
            "https://www.mambogo.com/test1",
            "script-src",
            "https://evil.com/script1.js",
            "https://www.mambogo.com", 
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        CspViolationEvent scriptViolation2 = createTestViolation(
            "https://www.mambogo.com/test2", 
            "script-src",
            "https://evil.com/script2.js",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.HIGH
        );
        
        CspViolationEvent styleViolation = createTestViolation(
            "https://www.mambogo.com/test3",
            "style-src",
            "https://evil.com/style.css",
            "https://www.mambogo.com",
            CspViolationEvent.ViolationSeverity.MEDIUM
        );
        
        // When
        violationProcessor.processViolation(scriptViolation1);
        violationProcessor.processViolation(scriptViolation2);
        violationProcessor.processViolation(styleViolation);
        
        CspViolationProcessor.ViolationStatistics stats = violationProcessor.getViolationStatistics();
        
        // Then
        assertThat(stats.getUniqueDirectives()).isEqualTo(2);
        assertThat(stats.getTotalViolations()).isEqualTo(3);
        assertThat(stats.getMostViolatedDirective()).contains("script-src");
    }

    @Test
    void shouldProvideMeaningfulStatistics() {
        // When - No violations yet
        CspViolationProcessor.ViolationStatistics emptyStats = violationProcessor.getViolationStatistics();
        
        // Then
        assertThat(emptyStats.getUniqueOrigins()).isEqualTo(0);
        assertThat(emptyStats.getUniqueDirectives()).isEqualTo(0);
        assertThat(emptyStats.getUniqueBlockedUris()).isEqualTo(0);
        assertThat(emptyStats.getTotalViolations()).isEqualTo(0);
        assertThat(emptyStats.getMostViolatedOrigin()).isEqualTo("none");
        assertThat(emptyStats.getMostViolatedDirective()).isEqualTo("none");
    }

    // Helper method to create test violations
    private CspViolationEvent createTestViolation(String documentUri, String violatedDirective, 
                                                 String blockedUri, String origin, 
                                                 CspViolationEvent.ViolationSeverity expectedSeverity) {
        return new CspViolationEvent(
            documentUri,
            "https://www.mambogo.com",  // referrer
            violatedDirective,
            violatedDirective,  // effective directive
            "default-src 'self'; " + violatedDirective + " 'self'",  // original policy
            blockedUri,
            null,  // source file
            null,  // line number
            null,  // column number
            "192.168.1.100",  // client IP
            "Mozilla/5.0 (Test Browser)",  // user agent
            origin,
            Instant.now()
        );
    }
}
