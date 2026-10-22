package com.teetech.authservice.listener;

import com.teetech.authservice.dto.event.CodeEmailRequestedEvent;
import com.teetech.authservice.enums.EmailPurpose;
import com.teetech.authservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

import static com.teetech.authservice.util.SecurityConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeEmailListener {
    private final EmailService emailService;

    @Async
    @TransactionalEventListener
    public void on(CodeEmailRequestedEvent event){
        try {
            emailService.sendEmail(event.recipient(), event.code(), metadataFor(event.purpose()));
        } catch (Exception e){
            log.error("Failed to send {} email to {}", event.purpose(), event.recipient(), e);
        }
    }

    private Map<String, Object> metadataFor(EmailPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> Map.of(
                    "template", "email/verification-email",
                    "subject", "Your Verification Code",
                    "expiryMinutes", VERIFICATION_CODE_EXPIRY,
                    "errorMessage", "Failed to send verification code email");
            case PASSWORD_RESET -> Map.of(
                    "template", "email/password-reset-email",
                    "subject", "Your Password Reset Code",
                    "expiryMinutes", PASSWORD_RESET_CODE_EXPIRY,
                    "errorMessage", "Failed to send password reset email");
        };
    }
}
