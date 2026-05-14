package com.cms.dto.response;

import java.util.UUID;

public record CreateMediaResponse(
        UUID id,
        String uploadUrl,
        String s3Key,
        long uploadUrlExpiresInSeconds
) {}
