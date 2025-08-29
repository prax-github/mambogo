package com.mambogo.product.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientRequest;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter((request, next) -> {
                    // Forward correlation ID from MDC if present
                    String correlationId = MDC.get("X-Correlation-Id");
                    if (correlationId != null && !correlationId.isEmpty()) {
                        return next.exchange(ClientRequest.from(request)
                                .header("X-Correlation-Id", correlationId)
                                .build());
                    }
                    return next.exchange(request);
                })
                .build();
    }
}
