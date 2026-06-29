package com.teetech.authservice.exception;

import lombok.Getter;

@Getter
public class AuthServiceException extends RuntimeException {
    private final String errorCode;

    public AuthServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
