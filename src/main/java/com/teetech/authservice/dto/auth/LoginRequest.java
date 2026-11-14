package com.teetech.authservice.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials for logging in")
public record LoginRequest(
        @Schema(description = "Account email. Trimmed and lowercased before use.",
                example = "alice@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Schema(description = "Account password",
                example = "CorrectHorseBattery1!",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password
)
{
        public LoginRequest{
                email = (email != null) ? email.trim().toLowerCase() : null;
        }

        @Override
        public String toString() {
                return "LoginRequest[email=" + email + ", password=***]";
        }
}