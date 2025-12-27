package com.example.auth_service.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.repository.VerificationTokenRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for the authentication flow.
 * These tests cover the entire authentication process, from signup to login,
 * using a real Spring Boot application context and an in-memory H2 database.
 *
 * <p>Scope: HTTP contract + security behavior for the core happy-path flow:
 * register → token created → verify → login → JWT gates protected endpoints.</p>
 *
 * <p>We intentionally validate externally observable behavior (status codes and response shape)
 * instead of repository internals wherever possible. The one exception is obtaining the verification
 * token from the repository because email delivery is handled by a different service.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("authdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @org.junit.jupiter.api.BeforeEach
    void initMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /**
     * Tests that a user can successfully sign up and then log in.
     * This test ensures that the signup and login endpoints work together correctly.
     */
    @Test
    void signup_then_login_succeeds() throws Exception {
        // Use a unique email per run to keep tests isolated and avoid uniqueness collisions.
        String email = "user_" + UUID.randomUUID() + "@example.com";
        RegistrationRequest signup = new RegistrationRequest(email, "Password123!", "Jane", "Doe");

        // Register: must return 201 and must not leak sensitive fields in the JSON payload.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.verificationToken").doesNotExist());

        // Pull the token directly from the DB to keep the test self-contained (email delivery is out-of-process).
        String token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals(email))
                .findFirst()
                .orElseThrow()
                .getToken();

        // Login should be blocked until verification completes.
        LoginRequest loginBeforeVerify = new LoginRequest(email, "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(loginBeforeVerify)))
                .andExpect(status().isForbidden());

        // Verify: should enable the account and consume the token.
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest(email, "Password123!");

        // Login: must return a JWT and role for downstream authorization decisions.
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn();

        String tokenJson = loginResult.getResponse().getContentAsString();
        String jwt = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(tokenJson)
                .get("token")
                .asText();

        // Protected route: missing JWT must be rejected (401).
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // Protected route: invalid/tampered JWT must be rejected (401) without revealing details.
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        // Protected route: valid JWT authenticates the request.
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
