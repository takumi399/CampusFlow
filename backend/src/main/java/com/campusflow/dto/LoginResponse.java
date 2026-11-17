package com.campusflow.dto;

public record LoginResponse(String token, long expiresInSeconds, UserSummary user) {
}
