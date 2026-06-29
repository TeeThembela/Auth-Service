package com.teetech.authservice.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {
    public AuthResponse(String accessToken,
                        Instant accessTokenExpiresAt,
                        Instant refreshTokenExpiresAt) {
        this(accessToken, "Bearer", accessTokenExpiresAt,
                refreshTokenExpiresAt);
    }
}
