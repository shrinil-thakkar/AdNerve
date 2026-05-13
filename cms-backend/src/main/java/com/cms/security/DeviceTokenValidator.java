package com.cms.security;

import com.cms.core.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class DeviceTokenValidator {

    private final JwtUtil jwtUtil;

    public DeviceTokenValidator(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public UUID validateAndExtractScreenId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing device token");
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            String tokenType = claims.get("type", String.class);
            if (!"device".equals(tokenType)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token type");
            }
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device token");
        }
    }
}
