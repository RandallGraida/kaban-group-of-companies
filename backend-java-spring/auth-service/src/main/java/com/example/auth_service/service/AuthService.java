package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegistrationRequest;
import com.example.auth_service.dto.RegistrationResponse;
import com.example.auth_service.dto.SignupRequest;

public interface AuthService {
    RegistrationResponse registerUser(RegistrationRequest request);
    void verifyUser(String token);

    // Backward-compatible alias for older clients.
    RegistrationResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}
