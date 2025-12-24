package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.dto.RegistrationResponse;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.exception.EmailNotVerifiedException;
import com.example.auth_service.exception.InvalidTokenException;
import com.example.auth_service.exception.TokenExpiredException;
import com.example.auth_service.exception.UserAlreadyExistsException;
import com.example.auth_service.model.UserAccount;
import com.example.auth_service.model.VerificationToken;
import com.example.auth_service.repository.VerificationTokenRepository;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.security.JwtUtil;
import com.example.auth_service.service.publisher.UserRegisteredPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the {@link AuthService} interface to provide authentication and user management services.
 * This class handles the business logic for user registration, email verification, and login.
 * It coordinates with repositories for data access, a password encoder for security, and a publisher to notify other services of user registration.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRegisteredPublisher userRegisteredPublisher;
    private final AuthenticationManager authenticationManager;
    private final AuthService self;

    public AuthServiceImpl(
            UserAccountRepository userRepository,
            VerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UserRegisteredPublisher userRegisteredPublisher,
            AuthenticationManager authenticationManager,
            @Lazy AuthService self) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userRegisteredPublisher = userRegisteredPublisher;
        this.authenticationManager = authenticationManager;
        this.self = self;
    }

    /**
     * Registers a new user, creates a verification token, and publishes a user registration event.
     *
     * @param request The registration request containing user details.
     * @return A response indicating the registration was successful.
     * @throws UserAlreadyExistsException if the email is already in use.
     */
    @Override
    @Transactional
    public RegistrationResponse registerUser(RegistrationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("Unable to register with provided credentials");
        }

        UserAccount user = new UserAccount();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setVerified(false);
        userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setCreatedAt(Instant.now());
        token.setConsumedAt(null);
        token.setRevokedAt(null);
        token.setExpiryDate(Instant.now().plusSeconds(24 * 60 * 60));
        tokenRepository.save(token);

        userRegisteredPublisher.publish(user.getEmail(), tokenValue);

        return new RegistrationResponse("Registration successful. Please verify your email.");
    }

    /**
     * Verifies a user's email address using the provided token.
     *
     * @param token The verification token sent to the user's email.
     * @throws InvalidTokenException if the token is invalid or expired.
     */
    @Override
    @Transactional
    public void verifyUser(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.getConsumedAt() != null || verificationToken.getRevokedAt() != null) {
            throw new InvalidTokenException("Invalid verification token");
        }
        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("Verification token has expired");
        }

        UserAccount user = verificationToken.getUser();
        user.setVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        verificationToken.setConsumedAt(Instant.now());
        tokenRepository.save(verificationToken);
    }

    /**
     * A convenience method that delegates to the main user registration logic.
     *
     * @param request The signup request.
     * @return A registration response.
     */
    @Override
    @Transactional
    public RegistrationResponse signup(SignupRequest request) {
        return self.registerUser(new RegistrationRequest(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        ));
    }

    /**
     * Authenticates a user and provides a JWT upon successful login.
     *
     * @param request The login request containing user credentials.
     * @return An authentication response with a JWT.
     * @throws BadCredentialsException if credentials are bad or the user is not verified.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (DisabledException e) {
            throw new EmailNotVerifiedException("Email not verified");
        }

        UserAccount user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) {
            throw new BadCredentialsException("User is inactive");
        }

        String token = jwtUtil.generate(user.getEmail(), Map.of("role", user.getRole()));
        return new AuthResponse(token, user.getRole(), jwtUtil.expiresAt().toString());
    }
}