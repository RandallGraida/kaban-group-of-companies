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
import java.util.Collection;
import java.util.List;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents a user account in the system.
 * This entity is mapped to the "users" table in the database.
 */
@Data
@Entity
@Table(name = "users")
public class UserAccount implements UserDetails {
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

    // Indicates whether the user has verified their email address.
    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "email_verification_last_sent_at")
    private Instant emailVerificationLastSentAt;

    @Column(name = "email_verification_send_count_24h", nullable = false)
    private int emailVerificationSendCount24h = 0;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return verified;
    }
}
