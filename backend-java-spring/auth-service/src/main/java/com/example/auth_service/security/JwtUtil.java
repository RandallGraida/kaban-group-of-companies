package com.example.auth_service.security;

import com.example.auth_service.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility class for handling JSON Web Tokens (JWTs).
 * This class provides methods for generating, parsing, and validating JWTs.
 * It uses a secure key for signing and verifying tokens.
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties properties;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a JWT for the given subject and claims.
     *
     * @param subject The subject of the token (typically the user's ID or email).
     * @param claims  A map of claims to include in the token payload.
     * @return The generated JWT as a string.
     */
    public String generate(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(properties.getExpiration())))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Parses a JWT and returns its claims.
     *
     * @param token The JWT to parse.
     * @return The {@link Claims} from the token.
     */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Calculates the expiration time for a new token.
     *
     * @return The {@link Instant} at which the token will expire.
     */
    public Instant expiresAt() {
        return Instant.now().plus(properties.getExpiration());
    }
}
