package com.example.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending verification emails to new users.
 * This class uses {@link JavaMailSender} to construct and send emails.
 * The email content is an HTML template with a verification link.
 */
@Service
@RequiredArgsConstructor
public class VerificationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.auth-base-url:http://localhost:8080}")
    private String authBaseUrl;

    @Value("${app.mail.from:no-reply@kaban.local}")
    private String fromAddress;

    /**
     * Sends a verification email to the specified email address.
     * The email contains a unique link that the user must click to verify their account.
     *
     * @param toEmail           The recipient's email address.
     * @param verificationToken The verification token to be included in the verification link.
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verifyLink = authBaseUrl + "/api/auth/verify?token=" + verificationToken;

        String subject = "Verify your Kaban account";
        String html = """
                <p>Welcome to Kaban.</p>
                <p>Please verify your email by clicking the link below:</p>
                <p><a href="%s">%s</a></p>
                <p>This link expires in 24 hours.</p>
                """.formatted(verifyLink, verifyLink);

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
            logger.warn("Failed to send verification email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
