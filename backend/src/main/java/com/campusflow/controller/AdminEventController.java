package com.campusflow.controller;

import com.campusflow.common.ApiResponse;
import com.campusflow.dto.AdminEventSummaryResponse;
import com.campusflow.dto.AdminEventDetailResponse;
import com.campusflow.dto.EventUpsertRequest;
import com.campusflow.security.UserPrincipal;
import com.campusflow.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ApiResponse<List<AdminEventSummaryResponse>> list() {
        return ApiResponse.ok(eventService.listAdminEvents());
    }

    @GetMapping("/{eventId}")
    public ApiResponse<AdminEventSummaryResponse> detail(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.getAdminEvent(eventId));
    }

    @GetMapping("/{eventId}/detail")
    public ApiResponse<AdminEventDetailResponse> fullDetail(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.getAdminEventDetail(eventId));
    }

    @PostMapping
    public ApiResponse<AdminEventSummaryResponse> create(
            @Valid @RequestBody EventUpsertRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(eventService.create(request, principal.id()), "活动草稿已创建");
    }

    @PutMapping("/{eventId}")
    public ApiResponse<AdminEventSummaryResponse> update(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpsertRequest request
    ) {
        return ApiResponse.ok(eventService.update(eventId, request), "活动草稿已更新");
    }

    @PostMapping("/{eventId}/publish")
    public ApiResponse<AdminEventSummaryResponse> publish(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.publish(eventId), "活动已发布");
    }

    @PostMapping("/{eventId}/cancel")
    public ApiResponse<AdminEventSummaryResponse> cancel(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.cancel(eventId), "活动已取消");
    }

    @PostMapping("/{eventId}/finish")
    public ApiResponse<AdminEventSummaryResponse> finish(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.finish(eventId), "活动已结束");
    }
}
