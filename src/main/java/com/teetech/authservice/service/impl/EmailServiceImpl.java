package com.teetech.authservice.service.impl;

import com.teetech.authservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static com.teetech.authservice.util.SecurityConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    @Value("${application.mail-sender}")
    private String sender;

    @Retryable(
            includes = MailSendException.class,
            maxRetriesString = "${application.mail.retry.max-retries:3}",
            delayString = "${application.mail.retry.delay:2000}",
            multiplierString = "${application.mail.retry.multiplier:2.0}"
    )
    @Override
    public void sendEmail(String recipient, String code, Map<String, Object> metadata) {
        log.debug("Sending email. recipient={}, template={}", recipient, metadata.get("template"));

        // Extract often used metadata
        String templateName = metadata.get("template").toString();
        String subject = metadata.get("subject").toString();

        try {

            // Prepare thr Thymeleaf context with template variables
            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("expiryMinutes", metadata.get("expiryMinutes"));

            // Render the template
            String html = templateEngine.process(templateName, context);

            // Create a MimeMessage and wrap it in a helper
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set standard email fields
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);

            // Hand it to JavaMailSender to send via SMTP
            mailSender.send(message);
            log.info("Email sent. template={}, subject={}", templateName, subject);

        } catch (MessagingException | MailException | TemplateEngineException e) {
            log.warn("Failed to send email. template={}, subject={}, error={}", templateName, subject, e.getMessage());
            throw new RuntimeException(metadata.get("errorMessage").toString(), e);
        }
    }
}
