package com.example.auth_service.service.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class HttpUserRegisteredPublisherTest {

    @Test
    void publish_posts_json_payload_to_notification_service() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(204));
            server.start();

            String baseUrl = server.url("/").toString().replaceAll("/$", "");
            HttpUserRegisteredPublisher publisher = new HttpUserRegisteredPublisher(
                    RestClient.builder(),
                    baseUrl
            );

            publisher.publish("user@example.com", "token-123");

            var request = server.takeRequest();
            assertThat(request.getPath()).isEqualTo("/internal/events/user-registered");
            assertThat(request.getHeader("Content-Type")).startsWith(MediaType.APPLICATION_JSON_VALUE);
            String body = request.getBody().readUtf8();
            assertThat(body).contains("\"email\":\"user@example.com\"");
            assertThat(body).contains("\"verificationToken\":\"token-123\"");
        }
    }
}
