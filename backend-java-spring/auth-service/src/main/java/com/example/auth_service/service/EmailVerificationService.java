package com.example.auth_service.service;

import com.example.auth_service.model.UserAccount;
import com.example.auth_service.model.VerificationToken;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.repository.VerificationTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service responsible for issuing, resending, and consuming email verification tokens.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofHours(24);

    private final VerificationTokenRepository tokenRepository;
    private final UserAccountRepository userRepository;
    private final VerificationEmailSender emailSender;

    /**
     * Sends a verification email for a newly created user.
     *
     * @param user newly created user account
     */
    @Transactional
    public void sendInitialVerificationEmail(UserAccount user) {
        if (user.isVerified()) {
            return;
        }
        upsertTokenAndSend(user);
    }

    /**
     * Resends a verification email for an unverified user.
     *
     * @param email user email address
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        UserAccount user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.isVerified()) {
            return;
        }
        upsertTokenAndSend(user);
    }

    /**
     * Verifies an email address using a raw token from a verification link.
     *
     * @param rawToken raw token provided by the user
     * @throws ResponseStatusException 400 for invalid/expired/used/revoked tokens
     */
    @Transactional
    public void verify(String rawToken) {
        String tokenHash = sha256Base64Url(rawToken);
        VerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token"));

        if (token.getConsumedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token already used");
        }
        if (token.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token expired");
        }

        UserAccount user = token.getUser();
        if (!user.isVerified()) {
            user.setVerified(true);
            user.setEmailVerifiedAt(Instant.now());
            userRepository.save(user);
        }

        token.setConsumedAt(Instant.now());
        tokenRepository.save(token);
    }

    /**
     * Creates or replaces the user's active verification token and sends the link via the sender.
     */
    private void upsertTokenAndSend(UserAccount user) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = sha256Base64Url(rawToken);

        VerificationToken token = tokenRepository.findByUser_Id(user.getId()).orElseGet(VerificationToken::new);
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plus(DEFAULT_TOKEN_TTL));
        token.setCreatedAt(Instant.now());
        token.setConsumedAt(null);
        token.setRevokedAt(null);
        tokenRepository.save(token);

        user.setEmailVerificationLastSentAt(Instant.now());
        user.setEmailVerificationSendCount24h(user.getEmailVerificationSendCount24h() + 1);
        userRepository.save(user);

        String link = "http://localhost:8080/api/auth/verify-email?token=" + rawToken;
        emailSender.sendVerificationEmail(user.getEmail(), link);
    }

    /**
     * Hashes a value using SHA-256 and encodes it using URL-safe base64 without padding.
     */
    private static String sha256Base64Url(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
