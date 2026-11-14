package com.teetech.authservice.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A request that only needs an email address")
public record EmailDTO(
        @Schema(description = "Account email address", example = "alice@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email
) {
}