package com.campusflow.dto;

import com.campusflow.domain.RegistrationStatus;

import java.time.LocalDateTime;

public record CheckInResponse(
        Long eventId,
        String eventTitle,
        RegistrationStatus status,
        LocalDateTime checkedInAt,
        String message
) {
}
