package com.campusflow.controller;

import com.campusflow.common.ApiResponse;
import com.campusflow.dto.EventDetailResponse;
import com.campusflow.dto.EventSummaryResponse;
import com.campusflow.security.UserPrincipal;
import com.campusflow.service.EventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ApiResponse<List<EventSummaryResponse>> list() {
        return ApiResponse.ok(eventService.listStudentEvents());
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventDetailResponse> detail(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(eventService.getStudentEvent(eventId, principal.id()));
    }
}
