package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.dto.RegistrationResponse;
import com.example.auth_service.dto.SignupRequest;
import com.example.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * Handles user registration requests.
     *
     * @param request A {@link SignupRequest} object containing user details.
     * @return A {@link ResponseEntity} with an {@link AuthResponse} containing the JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(201).body(authService.registerUser(request));
    }

    // Backward compatible alias
    @PostMapping("/signup")
    public ResponseEntity<RegistrationResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(authService.signup(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<RegistrationResponse> verify(@RequestParam("token") String token) {
        authService.verifyUser(token);
        return ResponseEntity.ok(new RegistrationResponse("Email verified successfully."));
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
