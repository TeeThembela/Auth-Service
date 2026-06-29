package com.teetech.authservice.service.impl;

import com.teetech.authservice.dto.auth.LoginRequest;
import com.teetech.authservice.dto.auth.RegisterRequest;
import com.teetech.authservice.dto.token.TokenResult;
import com.teetech.authservice.entity.Authority;
import com.teetech.authservice.entity.RefreshToken;
import com.teetech.authservice.entity.User;
import com.teetech.authservice.exception.EmailAlreadyExistsException;
import com.teetech.authservice.exception.InvalidTokenException;
import com.teetech.authservice.repository.AuthorityRepository;
import com.teetech.authservice.repository.RefreshTokenRepository;
import com.teetech.authservice.repository.UserRepository;
import com.teetech.authservice.security.model.UserPrincipal;
import com.teetech.authservice.service.AuthService;
import com.teetech.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    @Transactional
    @Override
    public void register(RegisterRequest request) {
        log.info("Registering username={}", request.email());

        // Check email is not already used
        if (userRepository.existsByUsername(request.email())){
            log.warn("Email already used. email={}", request.email());
            throw new EmailAlreadyExistsException("Email already exists", request.email());
        }

        // Fetch the authority
        Authority authority = authorityRepository.findByName("ROLE_USER")
                .orElseThrow(() -> {
                    log.warn("ROLE_USER not found.");
                    return new IllegalStateException("ROLE_USER not found — seed authorities first");
                });

        // Build the user
        User user = User.builder()
                .username(request.email())
                .password(passwordEncoder.encode(request.password()))
                .authorities(new HashSet<>(Set.of(authority)))
                .build();

        // Persist the user
        User saved = userRepository.save(user);

        log.info("Successfully registered the user. userId={}",saved.getId());
    }

    @Transactional
    @Override
    public Map<String, TokenResult> login(LoginRequest request) {
        log.info("Logging in a user. username={}", request.email());

        // Authenticate the user through authentication manager
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Build the tokens and return
        return buildTokenResponse((UserPrincipal) Objects.requireNonNull(auth.getPrincipal()));
    }

    @Transactional
    @Override
    public Map<String, TokenResult> refresh(String refreshToken) {
        log.info("Generating new access and refresh tokens");

        // Check if the refresh token is valid
        validateRefreshToken(refreshToken);

        // Fetch the target refresh token
        RefreshToken targetToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                   log.warn("Refresh token is not recognised, must've been revoked");
                   return new InvalidTokenException("Refresh token is not recognised, must've been revoked");
                });

        // Get the user through the stored refresh token
        User user = targetToken.getUser();

        // Revoke the refresh token
        refreshTokenRepository.delete(targetToken);

        // Wrap the user with UserPrincipal (e.i., adapter pattern)
        UserPrincipal userPrincipal = new UserPrincipal(user);

        // Build the tokens and return
        return buildTokenResponse(userPrincipal);
    }

    @Transactional
    @Override
    public void logout(String refreshToken) {
        log.info("Logging out a user");

        // Check if the refresh token is valid
        validateRefreshToken(refreshToken);

        // Fetch the target refresh token
        RefreshToken targetToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token is not recognised, must been revoked");
                    return new InvalidTokenException("Refresh token is not recognised, must've been revoked");
                });

        // Revoke the refresh token
        refreshTokenRepository.delete(targetToken);

        log.info("Successfully logged out the user and revoked the refresh token");
    }

    @Transactional
    @Override
    public void logoutAll(String username) {
        log.info("Logging out all devices that belong to this user");

        // Revoke all refresh that belong to this user
        refreshTokenRepository.deleteAllByUsername(username);

        log.info("Successfully logged out the user and revoked all their refresh tokens");
    }

    private Map<String, TokenResult> buildTokenResponse(UserPrincipal principal){
        log.debug("Building the tokens for userId={}", principal.getUserId());

        // Generate the access and refresh tokens
        TokenResult accessToken = jwtUtil.generateAccessToken(principal);
        TokenResult refreshToken = jwtUtil.generateRefreshToken(principal);

        // Build the refresh token
        RefreshToken token = RefreshToken.builder()
                .token(refreshToken.token())
                .user(principal.getUser())
                .expiresAt(refreshToken.expiresAt())
                .build();

        // Persist the refresh token
        RefreshToken saved = refreshTokenRepository.save(token);
        log.info("Successfully built the refresh token and persisted it. tokenId={}", saved.getId());

        // Build a response and return
        return new HashMap<>(Map.of("accessToken", accessToken, "refreshToken",
                refreshToken));
    }

    private void validateRefreshToken(String refreshToken){
        log.debug("Validating the refresh token");

        if (!jwtUtil.isRefreshTokenValid(refreshToken)){
            log.warn("Refresh token is invalid");
            throw new InvalidTokenException("Refresh token is invalid");
        }

        log.debug("The refresh token is valid");
    }
}
