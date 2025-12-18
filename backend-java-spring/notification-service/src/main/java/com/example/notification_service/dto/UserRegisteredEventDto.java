package com.example.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for user registration events.
 * This record encapsulates the data sent when a user registration event is published.
 * It is used by the notification service to send a verification email.
 */
public record UserRegisteredEventDto(
        /**
         * The email address of the newly registered user.
         * It must be a valid email format and cannot be blank.
         */
        @Email @NotBlank String email,

        /**
         * The verification token generated for the user.
         * This token is used to verify the user's email address and cannot be blank.
         */
        @NotBlank String verificationToken
) {}
