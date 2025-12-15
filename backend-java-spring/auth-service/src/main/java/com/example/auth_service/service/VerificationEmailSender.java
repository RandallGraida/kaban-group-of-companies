package com.example.auth_service.service;

/**
 * Abstraction for delivering email verification links.
 */
public interface VerificationEmailSender {

    /**
     * Sends a verification email containing a link that the user can click to verify ownership.
     *
     * @param email recipient email address
     * @param verificationLink absolute verification URL containing the token
     */
    void sendVerificationEmail(String email, String verificationLink);
}
