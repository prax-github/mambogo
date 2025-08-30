package com.mambogo.product.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorProperties errorProperties;

    public CustomAccessDeniedHandler(ErrorProperties errorProperties) {
        this.errorProperties = errorProperties;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(ErrorResponseConstants.CODE, errorProperties.getAuthorization().getCode());
        errorResponse.put(ErrorResponseConstants.MESSAGE, errorProperties.getAuthorization().getMessage());
        errorResponse.put(ErrorResponseConstants.TIMESTAMP, Instant.now().toString());
        errorResponse.put(ErrorResponseConstants.PATH, request.getRequestURI());
        errorResponse.put(ErrorResponseConstants.SERVICE, errorProperties.getServiceName());
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
