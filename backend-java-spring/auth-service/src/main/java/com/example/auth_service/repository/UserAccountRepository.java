package com.example.auth_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.auth_service.model.UserAccount;

/**
 * Spring Data JPA repository for {@link UserAccount} entities.
 * This interface provides methods for CRUD operations and custom queries on user accounts.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    /**
     * Finds a user account by their email address.
     *
     * @param email The email address to search for.
     * @return An {@link Optional} containing the {@link UserAccount} if found, or empty otherwise.
     */
    Optional<UserAccount> findByEmail(String email);

    /**
     * Checks if a user account with the given email address exists.
     *
     * @param email The email address to check.
     * @return {@code true} if an account with the email exists, {@code false} otherwise.
     */
    boolean existsByEmail(String email);
}
