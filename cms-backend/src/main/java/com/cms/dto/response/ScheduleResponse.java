package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ScheduleResponse(
        UUID screenId,
        String timezone,
        OffsetDateTime generatedAt,
        List<TimeSlotEntry> timeSlots,
        List<SequentialSlotEntry> sequentialSlots
) {
    public record TimeSlotEntry(
            UUID slotId,
            String name,
            String startTime,
            String endTime,
            List<Integer> daysOfWeek,
            List<MediaEntry> media
    ) {}

    public record SequentialSlotEntry(
            UUID slotId,
            String name,
            Integer sequenceOrder,
            Integer durationSeconds,
            List<MediaEntry> media
    ) {}

    public record MediaEntry(
            UUID assignmentId,
            UUID mediaId,
            String name,
            String mediaType,
            String url,
            Integer durationSeconds,
            Integer priority
    ) {}
}
