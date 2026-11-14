package com.teetech.authservice.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "An email address together with the 6-digit code that was sent to it")
public record CodeVerificationRequest(
        @Schema(description = "Email address the code was sent to", example = "alice@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Schema(description = "The 6-digit code from the email", example = "482951")
        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6 , message = "Verification code must be exactly 6 digits")
        String code
) {
        @Override
        public String toString() {
                return "CodeVerificationRequest[" + "email=" + email +
                        ", code=******]";
        }
}