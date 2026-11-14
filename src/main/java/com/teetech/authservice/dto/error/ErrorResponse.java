package com.teetech.authservice.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Error body returned by every failing endpoint")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Short error category", example = "Conflict")
        String error,

        @Schema(description = "HTTP status code", example = "409")
        int status,

        @Schema(description = "Explanation of what went wrong",
                example = "Email already exists: alice@example.com")
        String message,

        @Schema(description = "Request path that produced the error", example = "/api/v1/auth/register")
        String path,

        @Schema(description = "When the error occurred (UTC)", example = "2026-10-25T18:33:58.197Z")
        Instant timestamp,

        @Schema(description = "Extra detail. For validation errors, maps each invalid field to its message.",
                example = "{\"email\": \"Must be a valid email address\"}")
        Map<String, String> details
) {
    public ErrorResponse {
        if (timestamp == null) timestamp = Instant.now();
    }
}