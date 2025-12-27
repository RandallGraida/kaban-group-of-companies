package com.example.auth_service.dto;

/**
 * Simple response wrapper for endpoints that return a human-readable message.
 *
 * <p>Using a consistent shape (e.g., {@code {"message":"..."}}) makes it easier for clients to
 * handle non-token responses and display user-facing feedback.
 *
 * @param message client-facing message
 */
public record MessageResponse(String message) {}
