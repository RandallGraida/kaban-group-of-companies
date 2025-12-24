package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.dto.RegistrationResponse;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.service.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.*;

/**
 * REST controller for handling user authentication requests.
 * This controller provides endpoints for user registration (signup) and login.
 * It delegates the core logic of authentication to the {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Handles user registration requests.
     *
     * @param request A {@link SignupRequest} object containing user details.
     * @return A {@link ResponseEntity} with an {@link AuthResponse} containing the JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = authService.registerUser(request);
        return status(201).body(response);
    }

    /**
     * Handles user signup requests. This is a backward-compatible alias for the /register endpoint.
     *
     * @param request A {@link SignupRequest} object containing user details.
     * @return A {@link ResponseEntity} with an {@link AuthResponse} containing the JWT.
     */
    @PostMapping("/signup")
    public ResponseEntity<RegistrationResponse> signup(@Valid @RequestBody SignupRequest request) {
        RegistrationResponse response = authService.signup(request);
        return status(201).body(response);
    }

    /**
     * Verifies a user's account using a verification token.
     *
     * @param token The verification token received via email.
     * @return A {@link ResponseEntity} that redirects the user to the frontend login page with a success flag.
     */
    @GetMapping("/verify")
    public ResponseEntity<Void> verify(
            @RequestParam("token") 
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Invalid token format")
            String token
    ) {
        authService.verifyUser(token);
        // Redirect to frontend login page with success flag
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/auth/login?verified=true"))
                .build();
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