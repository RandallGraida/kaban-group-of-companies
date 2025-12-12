package com.example.auth_service.listener;

import com.example.auth_service.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends email verification messages after registration.
 * Uses the base URL configured in application.properties to build verification links.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(JavaMailSender.class)
public class EmailListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:${server.port}}")
    private String baseUrl;

    @Value("${app.mail.from:no-reply@kaban.local}")
    private String fromAddress;

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String verifyLink = baseUrl + "/api/auth/verify?token=" + event.getVerificationToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.getUser().getEmail());
        message.setSubject("Verify your Kaban account");
        message.setText(
                "Welcome to Kaban.\n\nPlease verify your email by clicking the link below:\n"
                        + verifyLink
                        + "\n\nThis link expires in 24 hours."
        );

        try {
            mailSender.send(message);
            logger.info("Sent verification email to {}", event.getUser().getEmail());
        } catch (Exception ex) {
            logger.warn("Failed to send verification email to {}: {}", event.getUser().getEmail(), ex.getMessage());
        }
    }
}
