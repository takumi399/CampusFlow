package com.campusflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.check-in")
public record CheckInProperties(String secret, int tokenTtlMinutes) {
}
