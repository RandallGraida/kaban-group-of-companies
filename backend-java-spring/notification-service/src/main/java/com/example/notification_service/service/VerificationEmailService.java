package com.example.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending verification emails to new users.
 * This class uses {@link JavaMailSender} to construct and send emails.
 * The email content is an HTML template with a verification link.
 *
 * <p>Operational notes:
 * <ul>
 *   <li>SMTP configuration is determined from Spring properties (e.g. {@code spring.mail.host}).</li>
 *   <li>For local development (MailHog), SMTP auth is typically disabled and username/password are omitted.</li>
 *   <li>For production SMTP, prefer setting properties via environment variables/secret manager and enabling
 *   TLS (STARTTLS) and auth as required by your provider.</li>
 * </ul>
 *
 * <p>Security notes:
 * <ul>
 *   <li>Verification tokens are secrets; logs must never emit the raw token.</li>
 *   <li>This service logs a masked link (token replaced with {@code ***}) and a short SHA-256 fingerprint
 *   to correlate requests without leaking the token.</li>
 * </ul>
 */
@Service
public class VerificationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;

    @Value("${app.auth-base-url:http://localhost:8080}")
    private String authBaseUrl;

    @Value("${app.mail.from:no-reply@kaban.local}")
    private String fromAddress;

    public VerificationEmailService(ObjectProvider<JavaMailSender> mailSenderProvider, Environment environment) {
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
    }

    /**
     * Sends a verification email to the specified email address.
     * The email contains a unique link that the user must click to verify their account.
     *
     * @param toEmail           The recipient's email address.
     * @param verificationToken The verification token to be included in the verification link.
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verifyLink = authBaseUrl + "/api/auth/verify?token=" + verificationToken;
        String maskedVerifyLink = authBaseUrl + "/api/auth/verify?token=***";
        String tokenFingerprint = fingerprint(verificationToken);

        String subject = "Verify your Kaban account";
        String html = """
                <p>Welcome to Kaban.</p>
                <p>Please verify your email by clicking the link below:</p>
                <p><a href="%s">%s</a></p>
                <p>This link expires in 24 hours.</p>
                """.formatted(verifyLink, verifyLink);

        if (!isSmtpConfigured()) {
            logger.info(
                    "SMTP is not configured; skipping email send. to={}, verifyLink={}, tokenFingerprint={}",
                    toEmail,
                    maskedVerifyLink,
                    tokenFingerprint
            );
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            logger.info(
                    "JavaMailSender is not configured; skipping email send. to={}, verifyLink={}",
                    toEmail,
                    maskedVerifyLink
            );
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            logger.info("Sent verification email to {}", toEmail);
        } catch (Exception ex) {
            logger.warn(
                    "Failed to send verification email to {} (verifyLink={}, tokenFingerprint={}): {}",
                    toEmail,
                    maskedVerifyLink,
                    tokenFingerprint,
                    ex.getMessage(),
                    ex
            );
        }
    }

    boolean isSmtpConfigured() {
        /*
         * Production-grade behavior:
         * - Require a host to be configured; without it, Spring Mail can't connect anywhere.
         * - Username/password are optional for local SMTP catchers (e.g. MailHog), but if either is set,
         *   require both to avoid partial/invalid auth configuration.
         */
        if (!environment.containsProperty("spring.mail.host")) {
            return false;
        }

        boolean hasUsername = environment.containsProperty("spring.mail.username");
        boolean hasPassword = environment.containsProperty("spring.mail.password");

        if (hasUsername != hasPassword) {
            return false;
        }

        return true;
    }

    private static String fingerprint(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8 && i < hash.length; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
