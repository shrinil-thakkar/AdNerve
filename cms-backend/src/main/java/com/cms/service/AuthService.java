package com.cms.service;

import com.cms.core.JwtUtil;
import com.cms.dto.request.DeviceTokenRequest;
import com.cms.dto.request.LoginRequest;
import com.cms.dto.response.TokenResponse;
import com.cms.repository.ScreenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ScreenRepository screenRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                       ScreenRepository screenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.screenRepository = screenRepository;
    }

    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtUtil.generateAdminToken(request.email());
        return new TokenResponse(token, "bearer");
    }

    public TokenResponse issueDeviceToken(DeviceTokenRequest request) {
        screenRepository.findByIdAndIsActiveTrue(request.screenId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        String token = jwtUtil.generateDeviceToken(request.screenId(), request.expiresDays());
        return new TokenResponse(token, "bearer");
    }
}
