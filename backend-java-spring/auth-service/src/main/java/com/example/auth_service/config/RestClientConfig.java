package com.example.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Centralizes construction of {@link RestClient} infrastructure for this service.
 *
 * <p>In production, consider extending this bean with:</p>
 * <ul>
 *   <li>Connection/read timeouts</li>
 *   <li>Retry/backoff policy</li>
 *   <li>Request/response logging with sensitive header redaction</li>
 *   <li>Tracing headers (correlation IDs)</li>
 * </ul>
 *
 * <p>Keeping the builder as a bean allows other components (like publishers) to share the same
 * configuration and makes integration testing easier.</p>
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        // Prefer injecting RestClient.Builder instead of instantiating RestClient directly
        // so cross-cutting concerns can be applied in one place.
        return RestClient.builder();
    }
}
