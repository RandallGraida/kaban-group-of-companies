package com.example.auth_service.exception;

/**
 * Thrown when an email verification token is validly formatted but expired.
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}

