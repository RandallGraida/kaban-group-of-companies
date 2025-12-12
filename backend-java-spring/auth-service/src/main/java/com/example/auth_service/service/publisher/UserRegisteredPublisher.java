package com.example.auth_service.service.publisher;

/**
 * Defines the contract for publishing user registration events.
 * This interface abstracts the mechanism of how user registration events are sent to other services.
 * Implementations of this interface can use various communication methods, such as HTTP, message queues (e.g., RabbitMQ, SQS), or event buses (e.g., SNS).
 */
public interface UserRegisteredPublisher {

    /**
     * Publishes an event indicating that a new user has registered.
     *
     * @param email             The email address of the newly registered user.
     * @param verificationToken The token generated for email verification.
     */
    void publish(String email, String verificationToken);
}
