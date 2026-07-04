package com.teetech.authservice.exception;

public class InvalidVerificationCodeException extends AuthServiceException {
  public InvalidVerificationCodeException(String message) {
    super(message, "INVALID_VERIFICATION_CODE");
  }
}
