package com.cms.dto.response;

public record TokenResponse(
        String accessToken,
        String tokenType
) {}
