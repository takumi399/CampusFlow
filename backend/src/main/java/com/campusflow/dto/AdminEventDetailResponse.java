package com.campusflow.dto;

import com.campusflow.domain.EventStatus;

import java.time.LocalDateTime;

public record AdminEventDetailResponse(
        Long id,
        String title,
        String description,
        String location,
        String category,
        String organizer,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        int capacity,
        int occupiedCount,
        int remainingSeats,
        EventStatus status,
        long confirmedCount,
        long waitlistedCount,
        long promotedCount,
        long checkedInCount
) {
}
