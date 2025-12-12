package com.example.auth_service.service;

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
import com.example.auth_service.repository.VerificationTokenRepository;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.security.JwtUtil;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the {@link AuthService} interface to provide authentication and user management services.
 * This class handles the business logic for user signup and login, including password hashing and JWT generation.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user in the system.
     *
     * @param request The {@link SignupRequest} containing the new user's details.
     * @return An {@link AuthResponse} containing a JWT for the newly created user.
     * @throws IllegalArgumentException if the email is already registered.
     */
    @Override
    @Transactional
    public RegistrationResponse registerUser(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(false);
        userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(24 * 60 * 60));
        tokenRepository.save(token);

        eventPublisher.publishEvent(new UserRegisteredEvent(this, user, tokenValue));

        return new RegistrationResponse("Registration successful. Please verify your email.");
    }

    @Override
    @Transactional
    public void verifyUser(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));
        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        UserAccount user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
    }

    @Override
    @Transactional
    public RegistrationResponse signup(SignupRequest request) {
        return registerUser(new RegistrationRequest(request.email(), request.password()));
    }

    /**
     * Authenticates a user and provides a JWT upon successful login.
     *
     * @param request The {@link LoginRequest} containing the user's credentials.
     * @return An {@link AuthResponse} containing a JWT for the authenticated user.
     * @throws BadCredentialsException if the credentials are invalid or the user is inactive.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (DisabledException e) {
            throw new BadCredentialsException("Email not verified");
        }

        UserAccount user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) {
            throw new BadCredentialsException("User is inactive");
        }

        String token = jwtUtil.generate(user.getEmail(), Map.of("role", user.getRole()));
        return new AuthResponse(token, user.getRole(), jwtUtil.expiresAt().toString());
    }
}
