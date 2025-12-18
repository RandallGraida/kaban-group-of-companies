package com.example.auth_service.exception;

/**
 * Thrown when an email verification token is missing or expired.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}

