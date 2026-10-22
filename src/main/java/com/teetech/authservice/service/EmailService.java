package com.teetech.authservice.service;

import java.util.Map;

public interface EmailService {
    void sendEmail(String recipient, String code, Map<String, Object> metadata);
}
