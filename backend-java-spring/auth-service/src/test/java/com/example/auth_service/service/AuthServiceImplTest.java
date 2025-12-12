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
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.dto.RegistrationResponse;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.event.UserRegisteredEvent;
import com.example.auth_service.exception.InvalidTokenException;
import com.example.auth_service.exception.UserAlreadyExistsException;
import com.example.auth_service.model.UserAccount;
import com.example.auth_service.model.VerificationToken;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.repository.VerificationTokenRepository;
import com.example.auth_service.security.JwtUtil;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        final AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this);
    }

    // Tests that a new user is created successfully and a token is returned.
    @Test
    void register_creates_disabled_user_and_token() {
        RegistrationRequest req = new RegistrationRequest("new@kaban.com", "Password123!");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse res = authService.registerUser(req);

        assertThat(res.message()).contains("verify");
        verify(userRepository).save(any(UserAccount.class));
        verify(tokenRepository).save(any(VerificationToken.class));
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    // Tests that an exception is thrown when trying to sign up with an email that already exists.
    @Test
    void register_throws_on_duplicate_email() {
        RegistrationRequest req = new RegistrationRequest("dup@kaban.com", "Password123!");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(req))
                .isInstanceOf(UserAlreadyExistsException.class);
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
        user.setEnabled(true);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(jwtUtil.generate(eq(req.email()), anyMap())).thenReturn("jwt-token");
        when(jwtUtil.expiresAt()).thenReturn(Instant.now().plusSeconds(3600));

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
        user.setEnabled(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // Tests that a login attempt with a bad password is rejected (via AuthenticationManager).
    @Test
    void login_rejects_bad_password() {
        LoginRequest req = new LoginRequest("user@kaban.com", "wrong");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void verifyUser_enables_user_and_deletes_token() {
        UserAccount user = new UserAccount();
        user.setEmail("user@kaban.com");
        user.setEnabled(false);
        VerificationToken token = new VerificationToken();
        token.setToken("t1");
        token.setExpiryDate(Instant.now().plusSeconds(60));
        token.setUser(user);

        when(tokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        authService.verifyUser("t1");

        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyUser_throws_on_expired_token() {
        VerificationToken token = new VerificationToken();
        token.setToken("t1");
        token.setExpiryDate(Instant.now().minusSeconds(1));
        token.setUser(new UserAccount());

        when(tokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyUser("t1"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
