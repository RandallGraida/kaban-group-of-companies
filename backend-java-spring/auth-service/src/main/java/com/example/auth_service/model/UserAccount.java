package com.example.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import lombok.Data;

/**
 * Represents a user account in the system.
 * This entity is mapped to the "users" table in the database.
 */
@Data
@Entity
@Table(name = "users")
public class UserAccount {
    // The unique identifier for the user account.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // The user's email address, which is unique and serves as the username.
    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    // The hashed password for the user account.
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // The role of the user, which determines their permissions.
    @NotBlank
    @Column(nullable = false)
    private String role = "ROLE_USER";

    // Whether the user's email has been verified.
    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    // Timestamp recorded when the user verifies their email.
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    // Used to support resend verification email throttling.
    @Column(name = "email_verification_last_sent_at")
    private Instant emailVerificationLastSentAt;

    // Rolling counter for verification email sends (implementation-defined window).
    @Column(name = "email_verification_send_count_24h", nullable = false, columnDefinition = "integer default 0")
    private int emailVerificationSendCount24h = 0;

    // A flag indicating whether the user account is active.
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
