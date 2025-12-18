package com.example.notification_service.controller;

import com.example.notification_service.dto.UserRegisteredEventDto;
import com.example.notification_service.service.VerificationEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling internal, service-to-service events.
 * This controller exposes endpoints that are intended to be called by other services within the system,
 * not by external users. It serves as a synchronous communication entry point for events like user registration.
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventsController {

    private static final Logger logger = LoggerFactory.getLogger(InternalEventsController.class);

    private final VerificationEmailService verificationEmailService;

    /**
     * Handles the 'user-registered' event.
     * When a user registers in the auth service, this endpoint is called to trigger a verification email.
     *
     * @param event The event payload containing the user's email and verification token.
     * @return A {@link ResponseEntity} indicating that the event has been accepted for processing.
     */
    @PostMapping("/user-registered")
    public ResponseEntity<Void> userRegistered(@Valid @RequestBody UserRegisteredEventDto event) {
        logger.info("Received user-registered event for email={}", event.email());
        verificationEmailService.sendVerificationEmail(event.email(), event.verificationToken());
        return ResponseEntity.accepted().build();
    }
}
