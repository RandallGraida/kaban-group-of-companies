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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for the authentication flow.
 * These tests cover the entire authentication process, from signup to login,
 * using a real Spring Boot application context and an in-memory H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

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
        String email = "user_" + UUID.randomUUID() + "@example.com";
        RegistrationRequest signup = new RegistrationRequest(email, "Password123!", "Jane", "Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.verificationToken").doesNotExist());

        String token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals(email))
                .findFirst()
                .orElseThrow()
                .getToken();

        LoginRequest loginBeforeVerify = new LoginRequest(email, "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(loginBeforeVerify)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest(email, "Password123!");

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

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
