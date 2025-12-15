package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.MessageResponse;
import com.example.auth_service.dto.ResendVerificationRequest;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.service.AuthService;
import com.example.auth_service.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling user authentication requests.
 * This controller provides endpoints for user registration (signup) and login.
 * It delegates the core logic of authentication to the {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Handles user registration requests.
     *
     * @param request A {@link SignupRequest} object containing user details.
     * @return A {@link ResponseEntity} with an {@link AuthResponse} containing the JWT.
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /**
     * Idempotently triggers a new verification email for an existing, unverified user.
     *
     * @param request payload containing the email to (re)verify
     * @return a generic success message regardless of whether the email is registered
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.email());
        return ResponseEntity.ok(new MessageResponse("If the email exists, a verification link was sent"));
    }

    /**
     * Confirms ownership of an email address using a one-time verification token.
     *
     * @param token raw verification token from the verification link
     * @return a success message when verification completes
     */
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.ok(new MessageResponse("Email verified"));
    }

    /**
     * Handles user login requests.
     *
     * @param request A {@link LoginRequest} object containing user credentials.
     * @return A {@link ResponseEntity} with an {@link AuthResponse} containing the JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
