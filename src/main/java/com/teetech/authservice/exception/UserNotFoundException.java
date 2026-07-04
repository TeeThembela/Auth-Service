package com.teetech.authservice.exception;

public class UserNotFoundException extends AuthServiceException {
    public UserNotFoundException(String message) {
        super(message, "USER_NOT_FOUND");
    }

    public UserNotFoundException(String message, String identifier) {
        super(message + ": " + identifier, "USER_NOT_FOUND");
    }
}
