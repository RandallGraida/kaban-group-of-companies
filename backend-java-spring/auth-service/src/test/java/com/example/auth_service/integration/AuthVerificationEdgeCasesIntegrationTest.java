package com.example.auth_service.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.model.UserAccount;
import com.example.auth_service.model.VerificationToken;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.repository.VerificationTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthVerificationEdgeCasesIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @BeforeEach
    void initMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void verification_token_is_single_use() throws Exception {
        String email = "user_" + UUID.randomUUID() + "@example.com";
        RegistrationRequest signup = new RegistrationRequest(email, "Password123!", "Jane", "Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(signup)))
                .andExpect(status().isCreated());

        String token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals(email))
                .findFirst()
                .orElseThrow()
                .getToken();

        mockMvc.perform(get("/api/auth/verify").param("token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/verify").param("token", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expired_token_returns_410_gone() throws Exception {
        UserAccount user = new UserAccount();
        user.setEmail("expired_" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setEnabled(false);
        userRepository.save(user);

        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().minusSeconds(5));
        tokenRepository.save(token);

        mockMvc.perform(get("/api/auth/verify").param("token", token.getToken()))
                .andExpect(status().isGone());
    }
}
