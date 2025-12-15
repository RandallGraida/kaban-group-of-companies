package com.example.auth_service.repository;

import com.example.auth_service.model.VerificationToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence API for {@link VerificationToken} entities.
 *
 * <p>Queries are optimized for the two primary operations:
 * looking up a token by its stored hash and finding the active token for a user.
 */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {

    /**
     * Finds a token by the persisted hash value (not the raw token).
     *
     * @param tokenHash SHA-256 hash encoded in URL-safe base64
     * @return token record if present
     */
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * Finds a user's token record (1:1 relationship).
     *
     * @param userId user id
     * @return token record if present
     */
    Optional<VerificationToken> findByUser_Id(String userId);
}
