package com.campusflow.controller;

import com.campusflow.common.ApiResponse;
import com.campusflow.dto.LoginRequest;
import com.campusflow.dto.LoginResponse;
import com.campusflow.dto.UserSummary;
import com.campusflow.security.UserPrincipal;
import com.campusflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "登录成功");
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(authService.toSummary(principal));
    }
}
