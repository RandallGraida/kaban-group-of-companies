package com.example.auth_service.controller;

import com.example.auth_service.exception.EmailNotVerifiedException;
import com.example.auth_service.exception.InvalidTokenException;
import com.example.auth_service.exception.TokenExpiredException;
import com.example.auth_service.exception.UserAlreadyExistsException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maps domain exceptions into clean JSON error responses for the auth API.
 *
 * <p>Goals:</p>
 * <ul>
 *   <li>Consistent status codes for clients and contract tests.</li>
 *   <li>No sensitive leakage (no stack traces or internal exception types in responses).</li>
 *   <li>Explicitly differentiate invalid vs expired verification tokens.</li>
 * </ul>
 */
@ControllerAdvice
public class AuthExceptionHandler {

    public record ErrorResponse(int status, String message, String timestamp) {}

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex) {
        // 409 indicates a conflict with a unique constraint (email).
        return new ResponseEntity<>(
                new ErrorResponse(409, ex.getMessage(), Instant.now().toString()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        // 400 for malformed/unknown verification tokens.
        return new ResponseEntity<>(
                new ErrorResponse(400, ex.getMessage(), Instant.now().toString()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpiredToken(TokenExpiredException ex) {
        // 410 indicates a previously valid resource is no longer usable (expired token).
        return new ResponseEntity<>(
                new ErrorResponse(410, ex.getMessage(), Instant.now().toString()),
                HttpStatus.GONE
        );
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex) {
        // 403 indicates the credentials may be correct, but the account state forbids login.
        return new ResponseEntity<>(
                new ErrorResponse(403, ex.getMessage(), Instant.now().toString()),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // 401 for invalid credentials / authentication failure.
        return new ResponseEntity<>(
                new ErrorResponse(401, ex.getMessage(), Instant.now().toString()),
                HttpStatus.UNAUTHORIZED
        );
    }
}