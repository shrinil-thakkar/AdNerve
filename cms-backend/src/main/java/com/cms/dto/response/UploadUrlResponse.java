package com.cms.dto.response;

public record UploadUrlResponse(
        String uploadUrl,
        String s3Key,
        long expiresInSeconds
) {}
