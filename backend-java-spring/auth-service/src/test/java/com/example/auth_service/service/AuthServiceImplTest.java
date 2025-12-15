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
import com.example.auth_service.dto.MessageResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

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

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        final AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this);
    }

    // Tests that a new user is created successfully and a verification email is triggered.
    @Test
    void signup_creates_user_and_returns_message() {
        SignupRequest req = new SignupRequest("new@kaban.com", "Password123!", "New", "User");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount u = invocation.getArgument(0);
            u.setId("id-1");
            return u;
        });

        MessageResponse res = authService.signup(req);

        assertThat(res.message()).containsIgnoringCase("verification");
        verify(userRepository).save(any(UserAccount.class));
        verify(emailVerificationService).sendInitialVerificationEmail(any(UserAccount.class));
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
        user.setVerified(true);
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
        user.setVerified(true);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_rejects_unverified_user_with_403() {
        LoginRequest req = new LoginRequest("user@kaban.com", "Password123!");
        UserAccount user = new UserAccount();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        user.setActive(true);
        user.setVerified(false);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // Tests that a login attempt with a bad password is rejected.
    @Test
    void login_rejects_bad_password() {
        LoginRequest req = new LoginRequest("user@kaban.com", "wrong");
        UserAccount user = new UserAccount();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        user.setActive(true);
        user.setVerified(true);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
