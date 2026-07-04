package com.teetech.authservice.exception;

public class TooManyRequestsException extends AuthServiceException {
    public TooManyRequestsException(String message) {
        super(message, "TOO_MANY_CODE_REQUESTS");
    }

    public TooManyRequestsException(String message, String identifier) {
        super(message + ": " + identifier, "TOO_MANY_CODE_REQUESTS");
    }
}
