package com.teetech.authservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeVerificationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6 , message = "Verification code must be exactly 6 digits")
        String code
) {
}
