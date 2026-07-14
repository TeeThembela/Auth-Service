package com.teetech.authservice.controller;

import com.teetech.authservice.dto.auth.AuthResponse;
import com.teetech.authservice.dto.auth.CodeVerificationRequest;
import com.teetech.authservice.dto.auth.LoginRequest;
import com.teetech.authservice.dto.auth.RegisterRequest;
import com.teetech.authservice.dto.common.ApiResponse;
import com.teetech.authservice.dto.token.TokenResult;
import com.teetech.authservice.security.model.UserPrincipal;
import com.teetech.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                        ApiResponse.success("Verify the email to complete registration." +
                                " Verification code sent to your email.")
                );
    }

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
                .path("/api/v1/auth/refresh")
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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@CookieValue(name = "refreshToken") String refreshToken,
                                                             HttpServletResponse response){

        Map<String, TokenResult> authResponse = authService.refresh(refreshToken);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", authResponse.get("refreshToken").token())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken") String refreshToken,
                                                    HttpServletResponse response){
        authService.logout(refreshToken);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logoutAll")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal principal,
                                          HttpServletResponse response){

        authService.logoutAll(principal.getUsername());

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(@RequestParam String email){
        authService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("Verification Code Sent"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyCode(@Valid @RequestBody CodeVerificationRequest request){
        authService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(ApiResponse.success("Email successfully verified"));
    }

}
