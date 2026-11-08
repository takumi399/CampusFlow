package com.campusflow.dto;

import com.campusflow.domain.RegistrationStatus;

import java.time.LocalDateTime;

public record AdminRegistrationResponse(
        Long id,
        Long userId,
        String username,
        String displayName,
        RegistrationStatus status,
        LocalDateTime waitlistTime,
        Integer waitlistPosition,
        LocalDateTime confirmDeadline,
        LocalDateTime checkedInAt,
        LocalDateTime createdAt,
        boolean canCheckIn
) {
}
