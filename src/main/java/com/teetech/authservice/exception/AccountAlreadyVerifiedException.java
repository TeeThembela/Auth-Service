package com.teetech.authservice.exception;

public class AccountAlreadyVerifiedException extends AuthServiceException {
    public AccountAlreadyVerifiedException(String message) {
        super(message, "ACCOUNT_ALREADY_VERIFIED");
    }

    public AccountAlreadyVerifiedException(String message, String identifier) {
        super(message + ": " + identifier, "ACCOUNT_ALREADY_VERIFIED");
    }
}
