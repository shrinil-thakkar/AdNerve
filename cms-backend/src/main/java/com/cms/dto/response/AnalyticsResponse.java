package com.cms.dto.response;

import java.util.List;
import java.util.UUID;

public record AnalyticsResponse(
        long totalPlays,
        long totalCompletions,
        String completionRate,
        List<MediaStats> byMedia
) {
    public record MediaStats(
            UUID mediaId,
            String name,
            long plays,
            long completions
    ) {}
}
