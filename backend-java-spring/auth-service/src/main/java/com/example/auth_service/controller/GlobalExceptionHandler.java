package com.example.auth_service.controller;

import com.example.auth_service.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception-to-HTTP mapping for the auth service.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles user-input and business-rule validation failures triggered inside the service layer.
     *
     * <p>Use {@link IllegalArgumentException} for client-correctable errors where the request is
     * syntactically valid but violates a rule (e.g., "email already registered").
     *
     * @param ex the thrown exception
     * @return a 400 response with a simple message payload
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Handles attempts to register a user that already exists.
     *
     * @param ex the thrown exception
     * @return a 409 response with the error message
     */
    @ExceptionHandler(com.example.auth_service.exception.UserAlreadyExistsException.class)
    public ResponseEntity<MessageResponse> handleUserAlreadyExists(com.example.auth_service.exception.UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Catch-all handler for unexpected errors.
     *
     * @param ex the thrown exception
     * @return a 500 response with the error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGeneralException(Exception ex) {
        ex.printStackTrace(); // Log to console
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Internal Error: " + ex.getMessage()));
    }

    /**
     * Handles authentication failures (e.g., invalid password, inactive account).
     *
     * <p>Returns 401 to indicate the client must authenticate with valid credentials. If you add
     * account lockout, MFA, or email verification enforcement, consider whether those cases should
     * return 401 vs 403 and whether the message should be generic.
     *
     * @param ex the thrown exception
     * @return a 401 response with a message payload
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(ex.getMessage()));
    }
}
