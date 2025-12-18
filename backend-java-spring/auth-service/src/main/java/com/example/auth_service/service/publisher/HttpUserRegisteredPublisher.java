package com.example.auth_service.service.publisher;

import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Publishes user registration events to the notification service via an HTTP POST request.
 * This implementation is responsible for notifying other parts of the system, like the notification service,
 * that a new user has registered. It sends the user's email and verification token to a configured endpoint.
 */
@Component
public class HttpUserRegisteredPublisher implements UserRegisteredPublisher {

    private static final Logger logger = LoggerFactory.getLogger(HttpUserRegisteredPublisher.class);

    private final RestClient restClient;
    private final URI endpoint;

    /**
     * Constructs the publisher with a {@link RestClient} and the target endpoint URL.
     *
     * @param restClientBuilder          A builder to create the {@link RestClient} instance.
     * @param notificationServiceBaseUrl The base URL of the notification service, configurable via application properties.
     */
    public HttpUserRegisteredPublisher(
            RestClient.Builder restClientBuilder,
            @Value("${notification-service.base-url:http://localhost:8084}") String notificationServiceBaseUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.endpoint = URI.create(notificationServiceBaseUrl + "/internal/events/user-registered");
    }

    /**
     * Publishes a user registration event by sending an HTTP POST request to the notification service.
     *
     * @param email             The email of the registered user.
     * @param verificationToken The verification token for the user.
     */
    @Override
    public void publish(String email, String verificationToken) {
        try {
            restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "verificationToken", verificationToken))
                    .retrieve()
                    .toBodilessEntity();
            logger.info("Successfully published user-registered event for email: {}", email);
        } catch (Exception ex) {
            logger.warn("Failed to publish user-registered event to notification-service: {}", ex.getMessage());
        }
    }
}
