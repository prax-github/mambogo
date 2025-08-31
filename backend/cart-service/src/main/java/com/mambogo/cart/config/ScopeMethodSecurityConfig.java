package com.mambogo.cart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuration for method-level security with custom scope evaluation
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class ScopeMethodSecurityConfig {

    private final ScopePermissionEvaluator scopePermissionEvaluator;
    private final JwtProperties jwtProperties;

    public ScopeMethodSecurityConfig(ScopePermissionEvaluator scopePermissionEvaluator, JwtProperties jwtProperties) {
        this.scopePermissionEvaluator = scopePermissionEvaluator;
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        CustomMethodSecurityExpressionHandler handler = new CustomMethodSecurityExpressionHandler(jwtProperties);
        handler.setPermissionEvaluator(scopePermissionEvaluator);
        return handler;
    }
}
