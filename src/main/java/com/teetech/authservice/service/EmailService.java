package com.teetech.authservice.service;

public interface EmailService {
    void sendEmailVerification(String recipient, String code);
}
