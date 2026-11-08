package com.campusflow.controller;

import com.campusflow.common.ApiResponse;
import com.campusflow.dto.RegistrationResponse;
import com.campusflow.security.UserPrincipal;
import com.campusflow.service.RegistrationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/events/{eventId}/registrations")
    public ApiResponse<RegistrationResponse> register(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(
                registrationService.register(eventId, principal.id()),
                "报名请求已处理"
        );
    }

    @DeleteMapping("/registrations/{registrationId}")
    public ApiResponse<RegistrationResponse> cancel(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(
                registrationService.cancel(registrationId, principal.id()),
                "报名已取消"
        );
    }

    @PostMapping("/registrations/{registrationId}/confirm")
    public ApiResponse<RegistrationResponse> confirm(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(
                registrationService.confirm(registrationId, principal.id()),
                "候补资格已确认"
        );
    }

    @GetMapping("/me/registrations")
    public ApiResponse<List<RegistrationResponse>> myRegistrations(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(registrationService.myRegistrations(principal.id()));
    }
}
