package com.teetech.authservice.exception;

public class EmailAlreadyExistsException extends AuthServiceException {
    public EmailAlreadyExistsException(String message) {
        super(message, "EMAIL_ALREADY_EXISTS");
    }

    public EmailAlreadyExistsException(String message, String identifier){
        super(message + ": " + identifier, "EMAIL_ALREADY_EXISTS");
    }
}
