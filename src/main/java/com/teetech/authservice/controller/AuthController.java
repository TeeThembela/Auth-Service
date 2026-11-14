package com.teetech.authservice.controller;

import com.teetech.authservice.config.OpenApiConfig;
import com.teetech.authservice.docs.ApiErrors;
import com.teetech.authservice.dto.auth.*;
import com.teetech.authservice.dto.common.ApiResponse;
import com.teetech.authservice.dto.token.TokenResult;
import com.teetech.authservice.security.model.UserPrincipal;
import com.teetech.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    private static final String REFRESH_COOKIE_SET =
            "`refreshToken` cookie: HttpOnly, Secure, SameSite=Strict, Path=/api/v1/auth, 7 days";
    private static final String REFRESH_COOKIE_CLEARED =
            "Expires the `refreshToken` cookie (Max-Age=0)";
    private static final String RESET_COOKIE_SET =
            "`passwordResetToken` cookie: HttpOnly, Secure, SameSite=Strict, "
                    + "Path=/api/v1/auth/reset-password, 10 minutes";
    private static final String RESET_COOKIE_CLEARED =
            "Expires the `passwordResetToken` cookie (Max-Age=0)";

    @Operation(
            tags = OpenApiConfig.TAG_AUTH,
            summary = "Register a new account",
            description = """
                    Creates an **unverified** account and emails a 6-digit verification code, \
                    valid for 15 minutes. The account cannot log in until the code is submitted \
                    to `POST /api/v1/auth/verify`. The email is trimmed and lowercased before \
                    it is stored.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Account created and verification code emailed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Verify the email to complete registration. Verification code sent to your email."
                                    }"""))))
    @ApiErrors.BadRequest
    @ApiErrors.Conflict
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Verify the email to complete registration." +
                        " Verification code sent to your email.")
        );
    }

    @Operation(
            tags = OpenApiConfig.TAG_AUTH,
            summary = "Log in",
            description = """
                    Verifies the email and password. On success the body carries a short-lived \
                    **access token** (send it as `Authorization: Bearer <token>`) and a long-lived \
                    **refresh token** is set as an `HttpOnly` cookie. Accounts that have not \
                    verified their email are rejected with 401.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Authenticated; refresh-token cookie set",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = REFRESH_COOKIE_SET,
                            schema = @Schema(type = "string")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Success",
                                      "data": {
                                        "accessToken": "eyJ0eXBlIjoiSldUIiwiYWxnIjoiSFMzODQifQ...",
                                        "tokenType": "Bearer",
                                        "accessTokenExpiresAt": "2026-10-25T18:49:17.921Z",
                                        "refreshTokenExpiresAt": "2026-11-01T18:34:17.921Z"
                                      }
                                    }"""))))
    @ApiErrors.BadRequest
    @ApiErrors.Unauthorized
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response){

        Map<String, TokenResult> authResponse = authService.login(request);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", authResponse.get("refreshToken").token())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(
                ApiResponse.success(
                        new AuthResponse(
                                authResponse.get("accessToken").token(),
                                authResponse.get("accessToken").expiresAt(),
                                authResponse.get("refreshToken").expiresAt()
                        )

                ));
    }

    @Operation(
            tags = OpenApiConfig.TAG_TOKENS,
            summary = "Refresh the access token",
            description = """
                    Reads the `refreshToken` cookie, **rotates** it, and returns a new access \
                    token. The previous refresh token stops working immediately, so replaying it \
                    returns 401. In Swagger UI the browser sends the cookie automatically once \
                    you have logged in.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "New access token issued; refresh-token cookie replaced",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = REFRESH_COOKIE_SET,
                            schema = @Schema(type = "string")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Success",
                                      "data": {
                                        "accessToken": "eyJ0eXBlIjoiSldUIiwiYWxnIjoiSFMzODQifQ...",
                                        "tokenType": "Bearer",
                                        "accessTokenExpiresAt": "2026-10-25T19:04:17.921Z",
                                        "refreshTokenExpiresAt": "2026-11-01T18:49:17.921Z"
                                      }
                                    }"""))))
    @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE)
    @ApiErrors.Unauthorized
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Parameter(hidden = true) @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse response){

        Map<String, TokenResult> authResponse = authService.refresh(refreshToken);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", authResponse.get("refreshToken").token())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(
                ApiResponse.success(
                        new AuthResponse(
                                authResponse.get("accessToken").token(),
                                authResponse.get("accessToken").expiresAt(),
                                authResponse.get("refreshToken").expiresAt()
                        )

                ));
    }

    @Operation(
            tags = OpenApiConfig.TAG_TOKENS,
            summary = "Log out of this device",
            description = """
                    Revokes the refresh token in the `refreshToken` cookie and clears the cookie. \
                    Access tokens that were already issued stay valid until they expire \
                    (short-lived by design).""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Logged out; refresh-token cookie cleared",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = REFRESH_COOKIE_CLEARED,
                            schema = @Schema(type = "string"))))
    @SecurityRequirement(name = OpenApiConfig.REFRESH_COOKIE)
    @ApiErrors.Unauthorized
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse response){
        authService.logout(refreshToken);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            tags = OpenApiConfig.TAG_TOKENS,
            summary = "Log out of every device",
            description = """
                    Revokes **all** refresh tokens that belong to the authenticated user and \
                    clears the cookie on this device. Requires a valid access token. Access \
                    tokens that were already issued stay valid until they expire.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "All sessions revoked; refresh-token cookie cleared",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = REFRESH_COOKIE_CLEARED,
                            schema = @Schema(type = "string"))))
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiErrors.Unauthorized
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logoutAll")
    public ResponseEntity<Void> logoutAll(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                          HttpServletResponse response){

        authService.logoutAll(principal.getUsername());

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            tags = OpenApiConfig.TAG_VERIFICATION,
            summary = "Resend the verification code",
            description = """
                    Issues a new 6-digit code for an **unverified** account. The new code replaces \
                    the previous one and is valid for 15 minutes. A new code can be requested \
                    only 2 minutes after the previous one.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "A new code was emailed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Verification Code Sent"
                                    }"""))))
    @ApiErrors.BadRequest
    @ApiErrors.NotFound
    @ApiErrors.Conflict
    @ApiErrors.TooManyRequests
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(
            @Parameter(description = "Email address of the account that needs a new code",
                    example = "alice@example.com", required = true)
            @RequestParam String email){
        authService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("Verification Code Sent"));
    }

    @Operation(
            tags = OpenApiConfig.TAG_VERIFICATION,
            summary = "Verify an email address",
            description = """
                    Submits the 6-digit code from the registration email and activates the \
                    account. A wrong or expired code returns 400; verifying an account that is \
                    already verified returns 409.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email verified; the account can now log in",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Email successfully verified"
                                    }"""))))
    @ApiErrors.BadRequest
    @ApiErrors.NotFound
    @ApiErrors.Conflict
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyCode(@Valid @RequestBody CodeVerificationRequest request){
        authService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success("Email successfully verified"));
    }

    @Operation(
            tags = OpenApiConfig.TAG_PASSWORD_RESET,
            summary = "Request a password reset code",
            description = """
                    Emails a 6-digit reset code, valid for 10 minutes, to a verified account. \
                    The response is **identical whether or not the account exists** (and whether \
                    or not an email was sent), so it cannot be used to discover which emails are \
                    registered.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Always returned for a well-formed request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "If an account exists with this email, a reset code has been sent"
                                    }"""))))
    @ApiErrors.BadRequest
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody EmailDTO request){
        authService.sendPasswordResetCode(request.email());
        return ResponseEntity.ok(ApiResponse.success("If an account exists with this email, a reset code has been sent"));
    }

    @Operation(
            tags = OpenApiConfig.TAG_PASSWORD_RESET,
            summary = "Verify a password reset code",
            description = """
                    Exchanges a valid reset code for a short-lived **password-reset cookie**, \
                    which `POST /api/v1/auth/reset-password` then requires. Every failure (unknown \
                    email, no reset requested, wrong code, expired code) returns the same 400 \
                    message, so the endpoint reveals nothing about which accounts exist.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Code accepted; password-reset cookie set",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = RESET_COOKIE_SET,
                            schema = @Schema(type = "string")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Reset code verified successfully"
                                    }"""))))
    @ApiErrors.BadRequest
    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiResponse<String>> verifyResetCode(@Valid @RequestBody CodeVerificationRequest request,
                                                               HttpServletResponse response){

        TokenResult token = authService.verifyPasswordResetCode(request.email(), request.code());

        ResponseCookie cookie = ResponseCookie
                .from("passwordResetToken", token.token())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/reset-password")
                .maxAge(Duration.ofMinutes(10))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Reset code verified successfully"));
    }

    @Operation(
            tags = OpenApiConfig.TAG_PASSWORD_RESET,
            summary = "Set a new password",
            description = """
                    Sets the new password using the password-reset cookie from \
                    `/verify-reset-code`. On success the cookie is cleared and **every refresh \
                    token of the account is revoked**, signing the user out on all devices.""",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password updated; reset cookie cleared; all sessions revoked",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = RESET_COOKIE_CLEARED,
                            schema = @Schema(type = "string")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Password successfully updated"
                                    }"""))))
    @SecurityRequirement(name = OpenApiConfig.RESET_COOKIE)
    @ApiErrors.BadRequest
    @ApiErrors.Unauthorized
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Parameter(hidden = true) @CookieValue(name = "passwordResetToken") String passwordResetToken,
            @Valid @RequestBody NewPasswordDTO request,
            HttpServletResponse response){
        authService.setNewPassword(passwordResetToken, request.password());

        ResponseCookie cookie = ResponseCookie
                .from("passwordResetToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/reset-password")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.success("Password successfully updated"));
    }
}