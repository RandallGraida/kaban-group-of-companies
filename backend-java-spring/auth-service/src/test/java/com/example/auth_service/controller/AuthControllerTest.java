package com.example.auth_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.dto.RegistrationResponse;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for the {@link AuthController}.
 * These tests use {@link MockMvc} to test the controller's endpoints without a running server.
 * The {@link AuthService} is mocked to isolate the controller's logic.
 */
@ActiveProfiles("test")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // Tests that the signup endpoint returns a 200 OK status and a token for a valid request.
    @Test
    void signup_returns_200_and_token() throws Exception {
        RegistrationResponse response = new RegistrationResponse("Registration successful. Please verify your email.");
        when(authService.registerUser(new RegistrationRequest("test@kaban.com", "Password123!")))
                .thenReturn(response);

        RegistrationRequest req = new RegistrationRequest("test@kaban.com", "Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());
    }

    // Tests that the login endpoint returns a 200 OK status and a token for a valid request.
    @Test
    void login_returns_200_and_token() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", "ROLE_USER", Instant.now().toString());
        when(authService.login(new LoginRequest("test@kaban.com", "Password123!"))).thenReturn(response);

        LoginRequest req = new LoginRequest("test@kaban.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    // Tests that the signup endpoint returns a 400 Bad Request status for an invalid payload.
    @Test
    void signup_returns_400_on_invalid_payload() throws Exception {
        String badPayload = """
                {"email":"","password":"short"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isBadRequest());
    }
}
