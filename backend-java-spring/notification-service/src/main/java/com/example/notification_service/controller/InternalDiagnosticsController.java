package com.example.notification_service.controller;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal diagnostics endpoints to help debug email delivery in local/dev environments.
 * These endpoints are intended for service-to-service or operator use, not public clients.
 *
 * <p>Production note: keep these endpoints behind network controls (e.g. private network, gateway allowlist)
 * or disable entirely outside of non-production environments.
 */
@RestController
@RequestMapping("/internal/diagnostics")
public class InternalDiagnosticsController {

    private final Environment environment;

    @Value("${app.auth-base-url:http://localhost:8080}")
    private String authBaseUrl;

    @Value("${app.mail.from:no-reply@kaban.local}")
    private String fromAddress;

    public InternalDiagnosticsController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/mail")
    public ResponseEntity<Map<String, Object>> mail() {
        /*
         * Only expose booleans and non-sensitive resolved values. Do not return credentials or secrets.
         * This endpoint is intended to answer: "Did the process load the expected configuration?"
         */
        boolean smtpHostConfigured = environment.containsProperty("spring.mail.host");
        boolean smtpPortConfigured = environment.containsProperty("spring.mail.port");
        boolean smtpUsernameConfigured = environment.containsProperty("spring.mail.username");
        boolean smtpPasswordConfigured = environment.containsProperty("spring.mail.password");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        payload.put("authBaseUrl", authBaseUrl);
        payload.put("fromAddress", fromAddress);
        payload.put("smtpHostConfigured", smtpHostConfigured);
        payload.put("smtpPortConfigured", smtpPortConfigured);
        payload.put("smtpUsernameConfigured", smtpUsernameConfigured);
        payload.put("smtpPasswordConfigured", smtpPasswordConfigured);
        payload.put("smtpHostValue", Objects.requireNonNull(environment.getProperty("spring.mail.host")));
        payload.put("smtpPortValue", Objects.requireNonNull(environment.getProperty("spring.mail.port")));
        return ResponseEntity.ok(payload);
    }
}
