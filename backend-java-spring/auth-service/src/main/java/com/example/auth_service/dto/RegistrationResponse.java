package com.example.auth_service.dto;

/**
 * Response returned after user registration. No JWT is issued until email verification completes.
 */
public record RegistrationResponse(String message) {}

