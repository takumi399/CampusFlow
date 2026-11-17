package com.campusflow.dto;

import com.campusflow.domain.UserRole;

public record UserSummary(Long id, String username, String displayName, UserRole role) {
}
