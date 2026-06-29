package com.teetech.authservice.dto.token;

import java.time.Instant;

public record TokenResult(
        String token,
        Instant expiresAt
) {
}
