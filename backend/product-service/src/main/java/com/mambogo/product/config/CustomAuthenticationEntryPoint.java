package com.mambogo.product.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorProperties errorProperties;

    public CustomAuthenticationEntryPoint(ErrorProperties errorProperties) {
        this.errorProperties = errorProperties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ErrorResponseConstants.CODE, errorProperties.getAuthentication().getCode());
        errorResponse.put(ErrorResponseConstants.MESSAGE, errorProperties.getAuthentication().getMessage());
        errorResponse.put(ErrorResponseConstants.TIMESTAMP, Instant.now().toString());
        errorResponse.put(ErrorResponseConstants.PATH, request.getRequestURI());
        errorResponse.put(ErrorResponseConstants.SERVICE, errorProperties.getServiceName());
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
