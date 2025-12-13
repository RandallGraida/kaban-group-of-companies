package com.example.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for registering a new user.
 * Kept minimal to avoid exposing the persistence model.
 */
public record RegistrationRequest(
        @Email @NotBlank String email,
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must include upper/lowercase, a number, and a symbol"
        )
        String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {}
