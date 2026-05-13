package com.cms.controller;

import com.cms.dto.request.DeviceTokenRequest;
import com.cms.dto.request.LoginRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.TokenResponse;
import com.cms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/device/token")
    public ResponseEntity<ApiResponse<TokenResponse>> issueDeviceToken(
            @Valid @RequestBody DeviceTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.issueDeviceToken(request)));
    }
}
