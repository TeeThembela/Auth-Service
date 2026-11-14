package com.teetech.authservice.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Details for creating a new account")
public record RegisterRequest(

        @Schema(description = "Email address for the new account. It becomes the login name and "
                + "receives the verification code. Trimmed and lowercased before use.",
                example = "alice@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Schema(description = "Account password, 12 to 128 characters",
                example = "CorrectHorseBattery1!",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password
) {
        public RegisterRequest{
                email = (email != null) ? email.trim().toLowerCase() : null;
        }

        @Override
        public String toString() {
                return "RegisterRequest[email=" + email + ", password=***]";
        }
}