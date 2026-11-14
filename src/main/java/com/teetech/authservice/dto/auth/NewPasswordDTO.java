package com.teetech.authservice.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "The new password for the account being recovered")
public record NewPasswordDTO(
        @Schema(description = "New password, 12 to 128 characters",
                example = "AnotherStrongPassword2!",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password
) {
        @Override
        public String toString() {
                return "NewPasswordDTO[password=***" + "]";
        }
}