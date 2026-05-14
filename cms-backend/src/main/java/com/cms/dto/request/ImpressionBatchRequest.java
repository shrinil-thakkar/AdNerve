package com.cms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ImpressionBatchRequest(
        @NotNull @Size(max = 500) List<@Valid ImpressionEntry> impressions
) {
    public record ImpressionEntry(
            @NotNull UUID mediaId,
            String category,
            Integer slotPosition,
            @NotNull OffsetDateTime playedAt,
            Integer durationPlayedSeconds,
            Boolean completed
    ) {}
}
