package com.teetech.authservice.service.impl;

import com.teetech.authservice.dto.auth.LoginRequest;
import com.teetech.authservice.dto.auth.RegisterRequest;
import com.teetech.authservice.dto.token.TokenResult;
import com.teetech.authservice.entity.Authority;
import com.teetech.authservice.entity.RefreshToken;
import com.teetech.authservice.entity.User;
import com.teetech.authservice.exception.*;
import com.teetech.authservice.repository.AuthorityRepository;
import com.teetech.authservice.repository.RefreshTokenRepository;
import com.teetech.authservice.repository.UserRepository;
import com.teetech.authservice.security.model.UserPrincipal;
import com.teetech.authservice.service.AuthService;
import com.teetech.authservice.service.EmailService;
import com.teetech.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.teetech.authservice.util.SecurityConstants.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final EmailService emailService;


    @Transactional
    @Override
    public void register(RegisterRequest request) {
        log.info("Registering username={}", request.email());

        // Check email is not already used
        String email = request.email().toLowerCase();
        if (userRepository.existsByUsername(email)){
            log.warn("Email already used. email={}", email);
            throw new EmailAlreadyExistsException("Email already exists", email);
        }

        // Fetch the authority
        Authority authority = authorityRepository.findByName("ROLE_USER")
                .orElseThrow(() -> {
                    log.warn("ROLE_USER not found.");
                    return new IllegalStateException("ROLE_USER not found — seed authorities first");
                });

        // Generate verification code and set expiration time
        String verificationCode = generateVerificationCode();
        Instant expiryAt = generateCodeExpiryMinutes(VERIFICATION_CODE_EXPIRY);

        // Build the user
        User user = User.builder()
                .username(email)
                .password(passwordEncoder.encode(request.password()))
                .authorities(new HashSet<>(Set.of(authority)))
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(expiryAt)
                .build();

        // Persist the user
        User saved = userRepository.save(user);

        // Compose email metadata
        Map<String, Object> mailMetadata = new HashMap<>();
        mailMetadata.put("template", "email/verification-email");
        mailMetadata.put("subject", "Your Verification Code");
        mailMetadata.put("expiryMinutes", VERIFICATION_CODE_EXPIRY);
        mailMetadata.put("errorMessage", "Failed to send verification code email");

        // Send verification
        emailService.sendEmailVerification(saved.getUsername(), verificationCode, mailMetadata);

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

        // Check if the token is expired
        if (targetToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(targetToken);
            log.warn("Refresh token has expired at={}", targetToken.getExpiresAt());
            throw new InvalidTokenException("Refresh token has expired");
        }

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

    @Transactional
    @Override
    public void sendVerificationCode(String username) {
        log.debug("Sending a verification code for username={}", username);

        // Fetch the user by username
        User user = findUserOrThrow(username);

        if (user.isEnabled()) {
            throw new AccountAlreadyVerifiedException("Account is already verified");
        }

        // Ensure user can only request a new code 2 minutes after the previous one
        if (user.getVerificationCodeExpiresAt() != null) {
            Instant cooldownEnd = user.getVerificationCodeExpiresAt()
                    .minus(VERIFICATION_CODE_EXPIRY - 2, ChronoUnit.MINUTES);

            if (Instant.now().isBefore(cooldownEnd)) {
                log.warn("Too many requests for a new code");
                throw new TooManyRequestsException("Please wait 2 minutes before requesting a new code");
            }
        }

        // Generate verification code and set expiration time
        String verificationCode = generateVerificationCode();
        Instant expiryAt = generateCodeExpiryMinutes(VERIFICATION_CODE_EXPIRY);
        
        // Update the user verification code and expiration time
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(expiryAt);
        
        // Persist the user
        userRepository.save(user);

        // Compose email metadata
        Map<String, Object> mailMetadata = new HashMap<>();
        mailMetadata.put("template", "email/verification-email");
        mailMetadata.put("subject", "Your Verification Code");
        mailMetadata.put("expiryMinutes", VERIFICATION_CODE_EXPIRY);
        mailMetadata.put("errorMessage", "Failed to send verification code email");
        
        // Send verification
        emailService.sendEmailVerification(username, verificationCode, mailMetadata);
        
        log.debug("Successfully sent a new verification, which expires at: {}", expiryAt);
    }

    @Transactional
    @Override
    public void sendPasswordResetCode(String username) {
        log.debug("Sending a password reset code for username={}", username);

        // Fetch the user
        Optional<User> optionalUser = userRepository.findByUsername(username.toLowerCase());
        if (optionalUser.isEmpty()) return;

        User user = optionalUser.get();

        // Check if the account is verified
        if (!user.isEnabled()) {
            return;
        }

        // Ensure user can only request a new code 2 minutes after the previous one
        if (user.getVerificationCodeExpiresAt() != null) {
            Instant cooldownEnd = user.getVerificationCodeExpiresAt()
                    .minus(PASSWORD_RESET_CODE_EXPIRY - 2, ChronoUnit.MINUTES);

            if (Instant.now().isBefore(cooldownEnd)) {
                log.warn("Too many requests for a new code");
                throw new TooManyRequestsException("Please wait 2 minutes before requesting a new code");
            }
        }

        // Generate verification code and set expiration time
        String verificationCode = generateVerificationCode();
        Instant expiryAt = generateCodeExpiryMinutes(PASSWORD_RESET_CODE_EXPIRY);

        // Update the user verification code and expiration time
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(expiryAt);

        // Persist the user
        userRepository.save(user);

        // Compose email metadata
        Map<String, Object> mailMetadata = new HashMap<>();
        mailMetadata.put("template", "email/password-reset-email");
        mailMetadata.put("subject", "Your Password Reset Code");
        mailMetadata.put("expiryMinutes", PASSWORD_RESET_CODE_EXPIRY);
        mailMetadata.put("errorMessage", "Failed to send password reset email");

        // Send verification
        emailService.sendEmailVerification(username, verificationCode, mailMetadata);

        log.debug("Successfully sent a new password rest code, which expires at: {}", expiryAt);
    }


    @Transactional
    @Override
    public void verifyCode(String username, String verificationCode) {
        log.debug("Verify code={} for username={}", verificationCode, username);
        
        // Fetch the user by username
        User user = findUserOrThrow(username);

        // Check if user account is enable before touching either field
        // if not, proceed, otherwise throw exception
        if (user.isEnabled()) {
            log.warn("Account is already verified");
            throw new AccountAlreadyVerifiedException("Account is already verified");
        }

        // Check the expiration
        if (user.getVerificationCodeExpiresAt().isBefore(Instant.now())){
            log.warn("Verification code expired at {}.", user.getVerificationCodeExpiresAt());
            throw new InvalidVerificationCodeException("Verification code expired");
        }

        // Check verification code matches
        if (!user.getVerificationCode().equals(verificationCode)){
            log.warn("Verification code doesn't match.");
            throw new InvalidVerificationCodeException("Verification code doesn't match.");
        }

        // Enable user account
        user.setEnabled(true);

        // Clear the user verification code and expiration time
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        // Persist the user
        userRepository.save(user);

        log.debug("Code successfully verified.");
    }

    @Transactional
    @Override
    public TokenResult verifyPasswordResetCode(String username, String verificationCode) {
        log.debug("Verify password reset code={} for username={}", verificationCode, username);

        // Fetch the user by username
        User user = null;
        try {
            user = findUserOrThrow(username);
        } catch (UserNotFoundException e) {
            log.warn("Invalid or expired reset code");
            throw new InvalidVerificationCodeException("Invalid or expired reset code");
        }

        // Check if the password reset was requested
        if (user.getVerificationCode() == null || user.getVerificationCodeExpiresAt() == null) {
            log.warn("No password reset was requested");
            throw new InvalidVerificationCodeException("No password reset was requested");
        }

        // Check the expiration
        if (user.getVerificationCodeExpiresAt().isBefore(Instant.now())){
            log.warn("Password reset code expired at {}.", user.getVerificationCodeExpiresAt());
            throw new InvalidVerificationCodeException("Password reset code expired");
        }

        // Check verification code matches
        if (!user.getVerificationCode().equals(verificationCode)){
            log.warn("Password reset code doesn't match.");
            throw new InvalidVerificationCodeException("Password reset code doesn't match.");
        }

        // Clear the user verification code and expiration time
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        // Persist the user
        userRepository.save(user);

        // Generate password reset token
        TokenResult token = jwtUtil.generatePasswordResetToken(new UserPrincipal(user));

        log.debug("Password reset code is successfully verified.");

        // return TokenResult
        return token;
    }

    @Transactional
    @Override
    public void setNewPassword(String passwordResetToken, String newPassword) {
        log.debug("Setting a new password");

        // Verify the password reset token
        if(!jwtUtil.isPasswordResetTokenValid(passwordResetToken)){
            log.warn("The password reset token is invalid");
            throw new InvalidTokenException("Password reset token is invalid");
        }

        // Extract the username from the token
        String username = jwtUtil.extractUsername(passwordResetToken);

        // Fetch the user
        User user = findUserOrThrow(username);

        // Set a new password
        user.setPassword(passwordEncoder.encode(newPassword));

        // Persist the user
        userRepository.save(user);

        log.debug("Successfully updated the password for user={}", username);

        // Delete all refresh tokens for this user
        // To force re-login on all devices
        logoutAll(username);
    }


    // HELPER METHODS

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

    private String generateVerificationCode(){
        log.debug("Generating a new verification code");

        SecureRandom random = new SecureRandom();

        // Generate a number between 0 and 999999
        int number = random.nextInt(1000000);

        // Ensure the number is exactly 6 digits and then return
        return String.format("%06d", number);
    }

    private Instant generateCodeExpiryMinutes(int codeExpiry) {
        return Instant.now().plus(codeExpiry, ChronoUnit.MINUTES);
    }

    private User findUserOrThrow(String username) {
        log.debug("Fetching a user with username={}", username);

        return userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> {
                    log.warn("User not found with username={}", username);
                    return new UserNotFoundException("User not found with username", username);
                });
    }

}
