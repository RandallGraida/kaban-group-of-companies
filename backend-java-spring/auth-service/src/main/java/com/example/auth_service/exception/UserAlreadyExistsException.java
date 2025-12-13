package com.example.auth_service.exception;

/**
 * Thrown when attempting to register an email that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

