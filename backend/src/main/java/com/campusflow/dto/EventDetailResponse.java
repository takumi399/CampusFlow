package com.campusflow.dto;

import com.campusflow.domain.EventStatus;

import java.time.LocalDateTime;

public record EventDetailResponse(
        Long id,
        String title,
        String description,
        String category,
        String organizer,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        int capacity,
        int occupiedCount,
        int remainingSeats,
        EventStatus status,
        RegistrationResponse registration
) {
}
