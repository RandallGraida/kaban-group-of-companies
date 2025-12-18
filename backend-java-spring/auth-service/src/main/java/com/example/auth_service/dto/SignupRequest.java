package com.example.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for user signup via the public API.
 *
 * <p>This DTO is deliberately small and only contains fields that are safe to accept from clients.
 * Notably, it does not accept role/flags fields to prevent mass-assignment (overposting).</p>
 */
public record SignupRequest(
        /**
         * User email. Normalization (e.g., lowercasing) is handled server-side.
         */
        @Email @NotBlank String email,
        /**
         * Raw password input. This is never persisted directly; the service hashes it (BCrypt).
         *
         * <p>Complexity rule: at least one lower, upper, digit, and symbol.</p>
         */
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must include upper/lowercase, a number, and a symbol"
        )
        String password,
        /**
         * KYC/display fields captured at signup; stored in downstream services if applicable.
         */
        @NotBlank String firstName,
        @NotBlank String lastName) {
}
