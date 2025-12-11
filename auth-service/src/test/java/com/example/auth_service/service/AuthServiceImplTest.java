package com.example.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.model.UserAccount;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.security.JwtUtil;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link AuthServiceImpl}.
 * These tests verify the business logic of the authentication service,
 * including user creation, duplicate email handling, password validation, and JWT generation.
 */
@ActiveProfiles("test")
class AuthServiceImplTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        final AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this);
    }

    // Tests that a new user is created successfully and a token is returned.
    @Test
    void signup_creates_user_and_returns_token() {
        SignupRequest req = new SignupRequest("new@kaban.com", "Password123!", "New", "User");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(jwtUtil.generate(eq(req.email()), anyMap())).thenReturn("jwt-token");
        when(jwtUtil.expiresAt()).thenReturn(java.time.Instant.now().plusSeconds(3600));
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount u = invocation.getArgument(0);
            u.setId("id-1");
            return u;
        });

        AuthResponse res = authService.signup(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.role()).isEqualTo("ROLE_USER");
        assertThat(res.expiresAt()).isNotBlank();
        verify(userRepository).save(any(UserAccount.class));
    }

    // Tests that an exception is thrown when trying to sign up with an email that already exists.
    @Test
    void signup_throws_on_duplicate_email() {
        SignupRequest req = new SignupRequest("dup@kaban.com", "Password123!", "Dup", "User");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any());
    }

    // Tests that a token is returned for valid login credentials.
    @Test
    void login_returns_token_for_valid_credentials() {
        LoginRequest req = new LoginRequest("user@kaban.com", "Password123!");
        UserAccount user = new UserAccount();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        user.setActive(true);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "hashed")).thenReturn(true);
        when(jwtUtil.generate(eq(req.email()), anyMap())).thenReturn("jwt-token");
        when(jwtUtil.expiresAt()).thenReturn(java.time.Instant.now().plusSeconds(3600));

        AuthResponse res = authService.login(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.role()).isEqualTo("ROLE_USER");
        assertThat(res.expiresAt()).isNotBlank();
    }

    // Tests that an inactive user is rejected during login.
    @Test
    void login_rejects_inactive_user() {
        LoginRequest req = new LoginRequest("user@kaban.com", "Password123!");
        UserAccount user = new UserAccount();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        user.setActive(false);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // Tests that a login attempt with a bad password is rejected.
    @Test
    void login_rejects_bad_password() {
        LoginRequest req = new LoginRequest("user@kaban.com", "wrong");
        UserAccount user = new UserAccount();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        user.setActive(true);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
