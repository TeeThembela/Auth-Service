package com.teetech.authservice.util;


public class SecurityConstants {
    private SecurityConstants(){}

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/send-code",
            "/api/v1/auth/verify"
    };
    public static final int VERIFICATION_CODE_EXPIRY = 15;
}
