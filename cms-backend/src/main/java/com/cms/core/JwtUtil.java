package com.cms.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpireMinutes;
    private final long deviceTokenExpireDays;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expire-minutes}") long accessTokenExpireMinutes,
            @Value("${app.jwt.device-token-expire-days}") long deviceTokenExpireDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
        this.deviceTokenExpireDays = deviceTokenExpireDays;
    }

    public String generateAdminToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("type", "admin")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpireMinutes * 60_000L))
                .signWith(secretKey)
                .compact();
    }

    public String generateDeviceToken(UUID screenId, int expiresDays) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(screenId.toString())
                .claim("type", "device")
                .claim("screenId", screenId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + (long) expiresDays * 86_400_000L))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    public String extractTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    public UUID extractScreenId(String token) {
        return UUID.fromString(parseToken(token).get("screenId", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
