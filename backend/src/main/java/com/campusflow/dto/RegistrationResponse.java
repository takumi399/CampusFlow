package com.campusflow.dto;

import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.EventStatus;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long eventId,
        String eventTitle,
        EventStatus eventStatus,
        LocalDateTime eventStartTime,
        LocalDateTime eventEndTime,
        String eventLocation,
        RegistrationStatus status,
        LocalDateTime waitlistTime,
        Integer waitlistPosition,
        LocalDateTime confirmDeadline,
        LocalDateTime checkedInAt,
        LocalDateTime createdAt,
        boolean canCancel,
        boolean canConfirm,
        boolean canCheckIn
) {
}
