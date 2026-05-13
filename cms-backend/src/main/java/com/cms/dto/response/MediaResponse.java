package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        String name,
        String mediaType,
        String s3Key,
        String s3Bucket,
        Long fileSizeBytes,
        Integer durationSeconds,
        String mimeType,
        String thumbnailS3Key,
        UUID uploadedBy,
        OffsetDateTime createdAt,
        String downloadUrl
) {}
