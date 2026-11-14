package com.teetech.authservice.docs;

import com.teetech.authservice.dto.error.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reusable OpenAPI error responses.
 *
 * <p>Each nested annotation bundles one Swagger {@code @ApiResponse} so controller methods can
 * declare their error cases in one short line. Every error in this service has the same JSON
 * shape ({@link ErrorResponse}), so they all reference that schema.
 *
 * <p>Swagger's annotation is written fully qualified because this project already has its own
 * {@code ApiResponse} DTO.
 */
public final class ApiErrors {

    private ApiErrors() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request: validation failed, the body or a required parameter is "
                    + "missing or malformed, or a submitted code is wrong or expired",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    public @interface BadRequest {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication failed or is missing: bad credentials, an unverified or "
                    + "disabled account, or a missing, expired or revoked token",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    public @interface Unauthorized {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No account exists for the given email",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    public @interface NotFound {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict with the current state of the account, for example the email "
                    + "is already registered or the account is already verified",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    public @interface Conflict {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "A new code was requested too soon after the previous one "
                    + "(2-minute cooldown)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    public @interface TooManyRequests {}
}