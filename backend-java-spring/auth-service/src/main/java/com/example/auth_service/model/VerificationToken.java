package com.example.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * Represents a one-time email verification token for a newly registered user.
 * A token is valid until {@link #expiryDate}. Once consumed, it is removed.
 */
@Data
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "created_at")
    @Nullable
    private Instant createdAt;

    @Column(name = "consumed_at")
    @Nullable
    private Instant consumedAt;

    @Column(name = "revoked_at")
    @Nullable
    private Instant revokedAt;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}

