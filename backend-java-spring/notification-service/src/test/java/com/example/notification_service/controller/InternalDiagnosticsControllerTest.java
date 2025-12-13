package com.example.notification_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link InternalDiagnosticsController}.
 * Validates the mail diagnostics endpoint returns correct configuration flags.
 */
class InternalDiagnosticsControllerTest {

    /**
     * Verifies that the mail diagnostics endpoint returns the expected configuration
     * state including base URL, sender address, and SMTP configuration flags.
     *
     * @throws Exception if MockMvc request execution fails
     */
    @Test
    void mail_diagnostics_returns_configuration_flags() throws Exception {
        // Initialize controller with mock environment
        InternalDiagnosticsController controller = new InternalDiagnosticsController(new MockEnvironment());
        
        // Inject configuration values using reflection to bypass constructor injection
        ReflectionTestUtils.setField(controller, "authBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(controller, "fromAddress", "no-reply@kaban.local");

        // Build standalone MockMvc instance for testing
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Perform GET request and validate response structure and values
        mockMvc.perform(get("/internal/diagnostics/mail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authBaseUrl").value("http://localhost:8080"))
                .andExpect(jsonPath("$.fromAddress").value("no-reply@kaban.local"))
                .andExpect(jsonPath("$.smtpHostConfigured").value(false))
                .andExpect(jsonPath("$.smtpUsernameConfigured").value(false))
                .andExpect(jsonPath("$.smtpPasswordConfigured").value(false));
    }
}
