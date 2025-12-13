package com.example.auth_service.exception;

/**
 * Thrown when a user attempts to authenticate before verifying their email address.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}

