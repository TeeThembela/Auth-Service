package com.teetech.authservice.service;

import com.teetech.authservice.dto.auth.LoginRequest;
import com.teetech.authservice.dto.auth.RegisterRequest;
import com.teetech.authservice.dto.token.TokenResult;

import java.util.Map;

public interface AuthService {
    void register(RegisterRequest request);
    Map<String, TokenResult> login(LoginRequest request);
    Map<String, TokenResult> refresh(String refreshToken);
    void logout(String refreshToken);
    void logoutAll(String username);
    void sendVerificationCode(String username);
    void verifyCode(String username, String verificationCode);
}
