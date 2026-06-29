package com.teetech.authservice.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        int status,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> details
) {
    public ErrorResponse {
        if (timestamp == null) timestamp = Instant.now();
    }
}
