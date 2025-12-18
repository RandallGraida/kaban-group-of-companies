package com.example.notification_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link VerificationEmailService}.
 * Validates email verification logic including SMTP configuration handling.
 */
class VerificationEmailServiceTest {

    /**
     * Verifies that email sending is skipped when SMTP is not configured.
     * This test ensures the service gracefully handles missing mail configuration
     * without attempting to send emails or throwing exceptions.
     */
    @Test
    void skips_sending_when_smtp_not_configured() {
        // Mock the JavaMailSender dependency
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        
        // Create an ObjectProvider wrapper that returns the mocked JavaMailSender
        ObjectProvider<JavaMailSender> provider = new ObjectProvider<>() {
            @Override
            public JavaMailSender getObject(Object... args) {
                return mailSender;
            }

            @Override
            public JavaMailSender getIfAvailable() {
                return mailSender;
            }

            @Override
            public JavaMailSender getIfUnique() {
                return mailSender;
            }

            @Override
            public JavaMailSender getObject() {
                return mailSender;
            }
        };

        // Initialize environment without SMTP properties
        Environment env = new MockEnvironment();
        VerificationEmailService service = new VerificationEmailService(provider, env);
        
        // Inject required configuration properties via reflection
        ReflectionTestUtils.setField(service, "authBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@kaban.local");

        // Execute the service method
        service.sendVerificationEmail("user@example.com", "token-123");

        // Assert that the mail sender was never called
        verifyNoInteractions(mailSender);
        
        // Confirm SMTP is marked as not configured
        assertThat(service.isSmtpConfigured()).isFalse();
    }
}
