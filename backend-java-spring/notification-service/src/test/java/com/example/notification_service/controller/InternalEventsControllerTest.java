package com.example.notification_service.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notification_service.service.VerificationEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalEventsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VerificationEmailService verificationEmailService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalEventsController(verificationEmailService)).build();
    }

    @Test
    void userRegistered_returns_202_and_sends_email() throws Exception {
        String payload = """
                {"email":"user@example.com","verificationToken":"token-123"}
                """;

        mockMvc.perform(post("/internal/events/user-registered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        verify(verificationEmailService).sendVerificationEmail("user@example.com", "token-123");
    }
}
