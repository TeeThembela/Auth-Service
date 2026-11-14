package com.teetech.authservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    /** Security scheme names, referenced by @SecurityRequirement on the endpoints that need them. */
    public static final String BEARER_AUTH = "bearerAuth";
    public static final String REFRESH_COOKIE = "refreshCookie";
    public static final String RESET_COOKIE = "resetCookie";

    /** Tag names, shared with the controller so the groups stay consistent. */
    public static final String TAG_AUTH = "Authentication";
    public static final String TAG_VERIFICATION = "Email Verification";
    public static final String TAG_TOKENS = "Tokens & Sessions";
    public static final String TAG_PASSWORD_RESET = "Password Reset";

    @Bean
    public OpenAPI teeGateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TeeGate Auth Service API")
                        .version("v1.0.0")
                        .contact(new Contact().name("TeeTech"))
                        .description("""
                                Stateless authentication service built with Spring Boot and Spring Security. \
                                It handles registration with email verification, JWT login, refresh-token \
                                rotation, logout, and password reset.

                                ## Typical flows

                                **Register and sign in**
                                1. `POST /api/v1/auth/register`: creates the account and emails a 6-digit code
                                2. `POST /api/v1/auth/verify`: submit the code to activate the account
                                3. `POST /api/v1/auth/login`: returns an access token and sets the refresh-token cookie

                                **Reset a forgotten password**
                                1. `POST /api/v1/auth/forgot-password`: emails a reset code \
                                (always returns 200, whether or not the account exists)
                                2. `POST /api/v1/auth/verify-reset-code`: exchanges the code for a short-lived reset cookie
                                3. `POST /api/v1/auth/reset-password`: sets the new password and revokes every \
                                refresh token, signing the user out on all devices

                                ## Authentication
                                - **Access token**: a short-lived JWT returned by login as `data.accessToken`. \
                                Send it as `Authorization: Bearer <token>`. Only protected endpoints need it; \
                                currently that is `POST /api/v1/auth/logoutAll`. \
                                Click **Authorize** above and paste the token to try it.
                                - **Refresh token**: a long-lived JWT kept in an `HttpOnly`, `Secure`, \
                                `SameSite=Strict` cookie and read by `/refresh` and `/logout`. \
                                It is rotated on every use, so each refresh invalidates the previous token.
                                - **Password-reset token**: a short-lived cookie issued by `/verify-reset-code` \
                                and accepted only by `/reset-password`.

                                Cookies are set by the server and sent automatically by the browser, so there \
                                is nothing to paste for them. Because they are `Secure`, use \
                                `http://localhost` (browsers allow it) or HTTPS.

                                Verification codes are 6 digits and expire after 15 minutes. \
                                Reset codes expire after 10 minutes.

                                ## Errors
                                Every error response uses the same JSON shape: \
                                `error`, `status`, `message`, `path`, `timestamp` and, where relevant, `details`.

                                ## Local demo
                                When running with Docker Compose, outgoing emails are captured by Mailpit at \
                                http://localhost:8025. Read the verification and reset codes there.
                                """))
                .tags(List.of(
                        new Tag().name(TAG_AUTH)
                                .description("Create an account and sign in."),
                        new Tag().name(TAG_VERIFICATION)
                                .description("Activate a new account with the code sent by email."),
                        new Tag().name(TAG_TOKENS)
                                .description("Refresh access tokens and sign out."),
                        new Tag().name(TAG_PASSWORD_RESET)
                                .description("Recover access to an account with an emailed code.")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token from `POST /api/v1/auth/login` "
                                        + "(`data.accessToken`). Paste the token only; "
                                        + "Swagger UI adds the `Bearer ` prefix."))
                        .addSecuritySchemes(REFRESH_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("refreshToken")
                                .description("HttpOnly refresh-token cookie set by login and refresh. "
                                        + "The browser sends it automatically."))
                        .addSecuritySchemes(RESET_COOKIE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("passwordResetToken")
                                .description("HttpOnly password-reset cookie set by "
                                        + "`/verify-reset-code`. The browser sends it automatically.")));
    }
}