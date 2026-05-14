package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ScheduleResponse(
        UUID screenId,
        String timezone,
        Integer defaultSlotLengthSeconds,
        OffsetDateTime generatedAt,
        List<CategoryEntry> categories
) {
    public record CategoryEntry(
            String category,
            String startTime,
            String endTime,
            boolean isActive,
            List<SlotEntry> slots
    ) {}

    public record SlotEntry(
            Integer position,
            UUID mediaId,
            String name,
            String mediaType,
            String url
    ) {}
}
