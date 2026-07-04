package com.teetech.authservice.service.impl;

import com.teetech.authservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static com.teetech.authservice.util.SecurityConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    @Value("${application.mail-sender}")
    private String sender;

    @Override
    public void sendEmailVerification(String recipient, String code) {

        // Set the html placeholders
        Context context = new Context();
        context.setVariable("code", formatCode(code));
        context.setVariable("expiryMinutes", VERIFICATION_CODE_EXPIRY);

        String html = templateEngine.process("email/verification-email", context);

        // Create a MimeMessage empty message container
        MimeMessage message = mailSender.createMimeMessage();
        try {

            // Wrap it in a helper
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set standard email fields
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject("Your Verification Code");
            helper.setText(html, true);

            // Hand it to JavaMailSender to send via SMTP
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String formatCode(String code) {
        return code.length() == 6
                ? code.substring(0, 3) + " " + code.substring(3)
                : code;
    }
}
