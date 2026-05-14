package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScreenResponse(
        UUID id,
        String name,
        String location,
        String timezone,
        Integer defaultSlotLengthSeconds,
        Integer defaultSlotCount,
        OffsetDateTime lastSeenAt,
        boolean online,
        boolean isActive
) {}
