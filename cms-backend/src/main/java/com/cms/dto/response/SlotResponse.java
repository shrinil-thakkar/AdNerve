package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID screenId,
        String name,
        String slotType,
        String startTime,
        String endTime,
        List<Integer> daysOfWeek,
        Integer sequenceOrder,
        Integer durationSeconds,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
