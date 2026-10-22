package com.teetech.authservice.dto.event;

import com.teetech.authservice.enums.EmailPurpose;

public record CodeEmailRequestedEvent(
        String recipient,
        String code,
        EmailPurpose purpose
) {
    @Override
    public String toString() {
        return "CodeEmailRequestedEvent{" +
                "purpose=" + purpose +
                '}';
    }
}
