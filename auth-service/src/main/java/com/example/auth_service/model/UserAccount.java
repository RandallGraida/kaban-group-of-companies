package com.example.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    // A flag indicating whether the user account is active.
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
