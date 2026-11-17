package com.campusflow.dto;

import com.campusflow.domain.EventStatus;

import java.time.LocalDateTime;

public record AdminEventSummaryResponse(
        Long id,
        String title,
        EventStatus status,
        LocalDateTime startTime,
        LocalDateTime registrationEnd,
        int capacity,
        int occupiedCount,
        int remainingSeats,
        long confirmedCount,
        long waitlistedCount,
        long promotedCount,
        long checkedInCount
) {
}
