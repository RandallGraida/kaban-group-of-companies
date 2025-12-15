package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.MessageResponse;
import com.example.auth_service.dto.SignupRequest;

/**
 * Public API for authentication-related operations exposed by the auth service.
 */
public interface AuthService {

    /**
     * Registers a new user account and initiates email verification.
     *
     * @param request signup details
     * @return a client-facing message about next steps
     */
    MessageResponse signup(SignupRequest request);

    /**
     * Authenticates a user and returns a short-lived access token.
     *
     * @param request login credentials
     * @return a token response including the user's role and expiry time
     */
    AuthResponse login(LoginRequest request);
}
