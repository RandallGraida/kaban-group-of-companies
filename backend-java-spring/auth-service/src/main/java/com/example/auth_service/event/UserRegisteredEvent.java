package com.example.auth_service.event;

import com.example.auth_service.model.UserAccount;
import org.springframework.context.ApplicationEvent;

/**
 * Application event published after a user registers.
 * Listeners can react (e.g., send verification email) without coupling to AuthService.
 */
public class UserRegisteredEvent extends ApplicationEvent {

    private final UserAccount user;
    private final String verificationToken;

    public UserRegisteredEvent(Object source, UserAccount user, String verificationToken) {
        super(source);
        this.user = user;
        this.verificationToken = verificationToken;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getVerificationToken() {
        return verificationToken;
    }
}

