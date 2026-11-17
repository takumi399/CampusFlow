package com.campusflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.waitlist")
public record WaitlistProperties(int confirmationMinutes, long expiryScanMs) {
}
