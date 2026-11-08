package com.campusflow.domain;

import java.time.LocalDateTime;

public class Registration {

    private Long id;
    private Long eventId;
    private Long userId;
    private RegistrationStatus status;
    private LocalDateTime waitlistTime;
    private LocalDateTime confirmDeadline;
    private LocalDateTime checkedInAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getWaitlistTime() {
        return waitlistTime;
    }

    public void setWaitlistTime(LocalDateTime waitlistTime) {
        this.waitlistTime = waitlistTime;
    }

    public LocalDateTime getConfirmDeadline() {
        return confirmDeadline;
    }

    public void setConfirmDeadline(LocalDateTime confirmDeadline) {
        this.confirmDeadline = confirmDeadline;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
