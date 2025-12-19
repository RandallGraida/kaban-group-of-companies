package com.example.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for resending an email verification link.
 *
 * @param email email address to verify
 */
public record ResendVerificationRequest(@Email @NotBlank String email) {}
