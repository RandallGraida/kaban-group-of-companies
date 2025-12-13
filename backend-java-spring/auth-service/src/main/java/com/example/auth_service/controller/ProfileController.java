package com.example.auth_service.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple authenticated endpoint used by clients to verify a JWT session.
 */
@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        return ResponseEntity.ok(
                Map.of(
                        "email", authentication.getName(),
                        "authorities", authentication.getAuthorities().stream().map(Object::toString).toList()
                )
        );
    }
}

