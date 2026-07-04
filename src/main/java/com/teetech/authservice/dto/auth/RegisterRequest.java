package com.teetech.authservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password
) {
        public RegisterRequest{
                email = (email != null) ? email.trim().toLowerCase() : null;
        }
}
