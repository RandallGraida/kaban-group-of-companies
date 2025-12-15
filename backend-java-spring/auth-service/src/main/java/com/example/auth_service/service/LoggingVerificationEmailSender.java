package com.example.auth_service.service;

import org.springframework.stereotype.Component;

/**
 * Development-only {@link VerificationEmailSender} that logs verification links to stdout.
 */
@Component
public class LoggingVerificationEmailSender implements VerificationEmailSender {
    @Override
    public void sendVerificationEmail(String email, String verificationLink) {
        System.out.println("Verification email -> " + email + " : " + verificationLink);
    }
}
