package com.example.auth_service.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal authenticated endpoint used by clients to validate a JWT session.
 *
 * <p>This endpoint is intentionally read-only and returns only non-sensitive identity attributes
 * derived from the authenticated principal. It is useful for:</p>
 * <ul>
 *   <li>Frontend "session check" on app load.</li>
 *   <li>Manual verification of JWT middleware behavior.</li>
 *   <li>Smoke tests that require a protected route.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        // Authentication is provided by Spring Security once JwtAuthenticationFilter succeeds.
        return ResponseEntity.ok(
                Map.of(
                        "email", authentication.getName(),
                        "authorities", authentication.getAuthorities().stream().map(Object::toString).toList()
                )
        );
    }
}
