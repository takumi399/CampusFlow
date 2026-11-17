package com.campusflow.controller;

import com.campusflow.common.ApiResponse;
import com.campusflow.dto.CheckInResponse;
import com.campusflow.dto.CheckInTokenResponse;
import com.campusflow.security.UserPrincipal;
import com.campusflow.service.CheckInService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/admin/events/{eventId}/check-in-token")
    public ApiResponse<CheckInTokenResponse> generateToken(@PathVariable Long eventId) {
        return ApiResponse.ok(checkInService.generateToken(eventId), "签到二维码已生成");
    }

    @PostMapping("/check-in/{token}")
    public ApiResponse<CheckInResponse> checkIn(
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(checkInService.checkIn(token, principal.id()));
    }
}
