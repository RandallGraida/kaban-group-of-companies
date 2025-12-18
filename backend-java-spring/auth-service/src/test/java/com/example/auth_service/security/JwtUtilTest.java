package com.example.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth_service.config.JwtProperties;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract-level unit tests for {@link JwtUtil}.
 *
 * <p>These tests intentionally validate observable token behavior (claims + expiration window)
 * rather than implementation details. This keeps the suite resilient to refactors while still
 * protecting the security contract expected by the authentication layer.</p>
 */
class JwtUtilTest {

    @Test
    void generate_includes_subject_role_and_expiration() {
        // Use an explicit secret and expiration to avoid relying on environment defaults in tests.
        // HS256 requires a sufficiently long key; 32 ASCII chars = 256 bits.
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setExpiration(Duration.ofMinutes(5));

        JwtUtil jwtUtil = new JwtUtil(properties);

        // Act: issue a token with a minimal claim set used by the app (subject + role).
        String token = jwtUtil.generate("user@example.com", Map.of("role", "ROLE_USER"));
        Claims claims = jwtUtil.parse(token);

        // Assert: subject and role survive round-trip parsing.
        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("role")).isEqualTo("ROLE_USER");

        // Assert: token expiry is in the expected time window.
        // We allow a small buffer due to test execution time.
        Instant exp = claims.getExpiration().toInstant();
        assertThat(exp).isAfter(Instant.now());
        assertThat(exp).isBefore(Instant.now().plus(Duration.ofMinutes(6)));
    }
}
