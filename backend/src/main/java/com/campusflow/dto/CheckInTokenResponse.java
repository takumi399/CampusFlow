package com.campusflow.dto;

import java.time.LocalDateTime;

public record CheckInTokenResponse(String token, LocalDateTime expiresAt) {
}
