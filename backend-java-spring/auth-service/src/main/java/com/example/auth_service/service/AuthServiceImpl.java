package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.MessageResponse;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.model.UserAccount;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.security.JwtUtil;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements the {@link AuthService} interface to provide authentication and user management services.
 * This class handles the business logic for user signup and login, including password hashing and JWT generation.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    /**
     * Registers a new user in the system.
     *
     * @param request The {@link SignupRequest} containing the new user's details.
     * @return An {@link AuthResponse} containing a JWT for the newly created user.
     * @throws IllegalArgumentException if the email is already registered.
     */
    @Override
    @Transactional
    public MessageResponse signup(SignupRequest request) {
        /*
         * Signup is a two-step process:
         * 1) Create the account in an unverified state.
         * 2) Send a verification link out-of-band and return a generic message to the client.
         */
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        UserAccount user = new UserAccount();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        emailVerificationService.sendInitialVerificationEmail(user);
        return new MessageResponse("Verification email sent");
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
        UserAccount user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!user.isActive()) {
            throw new BadCredentialsException("User is inactive");
        }
        if (!user.isEnabled()) {
            // Email verification is required before issuing tokens for the account.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email not verified");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtUtil.generate(user.getEmail(), Map.of("role", user.getRole()));
        return new AuthResponse(token, user.getRole(), jwtUtil.expiresAt().toString());
    }
}
