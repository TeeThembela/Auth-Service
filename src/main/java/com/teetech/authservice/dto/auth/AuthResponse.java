package com.teetech.authservice.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Tokens issued after a successful login or refresh. The refresh token itself "
        + "is not in the body; it is set as an HttpOnly cookie.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        @Schema(description = "Short-lived JWT access token. Send it as `Authorization: Bearer <token>`.",
                example = "eyJ0eXBlIjoiSldUIiwiYWxnIjoiSFMzODQifQ...")
        String accessToken,

        @Schema(description = "Token type, always `Bearer`", example = "Bearer")
        String tokenType,

        @Schema(description = "When the access token expires (UTC)",
                example = "2026-10-25T18:49:17.921Z")
        Instant accessTokenExpiresAt,

        @Schema(description = "When the refresh token cookie expires (UTC)",
                example = "2026-11-01T18:34:17.921Z")
        Instant refreshTokenExpiresAt
) {
    public AuthResponse(String accessToken,
                        Instant accessTokenExpiresAt,
                        Instant refreshTokenExpiresAt) {
        this(accessToken, "Bearer", accessTokenExpiresAt,
                refreshTokenExpiresAt);
    }

    @Override
    public String toString() {
        return "AuthResponse[" +
                "accessToken=***" +
                ", tokenType=" + tokenType +
                ", accessTokenExpiresAt=" + accessTokenExpiresAt +
                ", refreshTokenExpiresAt=" + refreshTokenExpiresAt +
                "]";
    }
}