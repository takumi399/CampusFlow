package com.campusflow.service;

import com.campusflow.common.ApiException;
import com.campusflow.dto.LoginRequest;
import com.campusflow.dto.LoginResponse;
import com.campusflow.dto.UserSummary;
import com.campusflow.security.JwtService;
import com.campusflow.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(principal);
            return new LoginResponse(token, jwtService.expiresInSeconds(), toSummary(principal));
        } catch (AuthenticationException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
    }

    public UserSummary toSummary(UserPrincipal principal) {
        return new UserSummary(principal.id(), principal.username(), principal.displayName(), principal.role());
    }
}
