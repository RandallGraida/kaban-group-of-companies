package com.example.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth_service.config.JwtProperties;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    @Test
    void generate_includes_subject_role_and_expiration() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setExpiration(Duration.ofMinutes(5));

        JwtUtil jwtUtil = new JwtUtil(properties);

        String token = jwtUtil.generate("user@example.com", Map.of("role", "ROLE_USER"));
        Claims claims = jwtUtil.parse(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("role")).isEqualTo("ROLE_USER");

        Instant exp = claims.getExpiration().toInstant();
        assertThat(exp).isAfter(Instant.now());
        assertThat(exp).isBefore(Instant.now().plus(Duration.ofMinutes(6)));
    }
}

