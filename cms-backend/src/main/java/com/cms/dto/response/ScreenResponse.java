package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScreenResponse(
        UUID id,
        String name,
        String location,
        String deviceToken,
        String timezone,
        Boolean isActive,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean isOnline
) {}
