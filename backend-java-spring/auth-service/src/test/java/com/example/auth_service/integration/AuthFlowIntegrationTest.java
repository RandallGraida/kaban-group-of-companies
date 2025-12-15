package com.example.auth_service.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.AuthServiceApplication;
import com.example.auth_service.model.VerificationToken;
import com.example.auth_service.repository.VerificationTokenRepository;
import com.example.auth_service.service.VerificationEmailSender;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for the authentication flow.
 * These tests cover the entire authentication process, from signup to login,
 * using a real Spring Boot application context and an in-memory H2 database.
 */
@SpringBootTest(classes = AuthServiceApplication.class)
@ActiveProfiles("test")
@Transactional
@Import(AuthFlowIntegrationTest.TestConfig.class)
class AuthFlowIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private CapturingVerificationEmailSender emailSender;

    @org.junit.jupiter.api.BeforeEach
    void initMockMvc() {
        emailSender.reset();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    /**
     * Tests that a user can successfully sign up and then log in.
     * This test ensures that the signup and login endpoints work together correctly.
     */
    @Test
    void happy_path_user_verifies_then_logs_in() throws Exception {
        String email = "user_" + UUID.randomUUID() + "@example.com";
        SignupRequest signup = new SignupRequest(email, "Password123!", "Jane", "Doe");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        String token = captureTokenFromEmail(email);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified"));

        LoginRequest login = new LoginRequest(email, "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void negative_path_unverified_user_login_returns_403() throws Exception {
        String email = "user_" + UUID.randomUUID() + "@example.com";
        SignupRequest signup = new SignupRequest(email, "Password123!", "Jane", "Doe");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest(email, "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(login)))
                .andExpect(status().isForbidden());
    }

    @Test
    void edge_case_token_expired_returns_400() throws Exception {
        String email = "user_" + UUID.randomUUID() + "@example.com";
        SignupRequest signup = new SignupRequest(email, "Password123!", "Jane", "Doe");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isOk());

        String token = captureTokenFromEmail(email);
        VerificationToken dbToken = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals(email))
                .findFirst()
                .orElseThrow();
        dbToken.setExpiresAt(Instant.now().minusSeconds(5));
        tokenRepository.save(dbToken);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edge_case_token_already_used_returns_400() throws Exception {
        String email = "user_" + UUID.randomUUID() + "@example.com";
        SignupRequest signup = new SignupRequest(email, "Password123!", "Jane", "Doe");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isOk());

        String token = captureTokenFromEmail(email);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", token))
                .andExpect(status().isBadRequest());
    }

    private String captureTokenFromEmail(String expectedEmail) {
        String actualEmail = emailSender.getLastEmail();
        String link = emailSender.getLastLink();
        if (actualEmail == null || link == null) {
            throw new AssertionError("No verification email was sent");
        }
        if (!expectedEmail.equals(actualEmail)) {
            throw new AssertionError("Expected verification email to " + expectedEmail + " but got " + actualEmail);
        }
        int idx = link.indexOf("token=");
        if (idx < 0) {
            throw new AssertionError("Verification link missing token param: " + link);
        }
        return link.substring(idx + "token=".length());
    }

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        CapturingVerificationEmailSender capturingVerificationEmailSender() {
            return new CapturingVerificationEmailSender();
        }
    }

    static class CapturingVerificationEmailSender implements VerificationEmailSender {
        private final AtomicReference<String> lastEmail = new AtomicReference<>();
        private final AtomicReference<String> lastLink = new AtomicReference<>();

        @Override
        public void sendVerificationEmail(String email, String verificationLink) {
            lastEmail.set(email);
            lastLink.set(verificationLink);
        }

        void reset() {
            lastEmail.set(null);
            lastLink.set(null);
        }

        String getLastEmail() {
            return lastEmail.get();
        }

        String getLastLink() {
            return lastLink.get();
        }
    }
}
