package com.cms.service;

import com.cms.dto.response.AnalyticsResponse;
import com.cms.repository.ImpressionLogRepository;
import com.cms.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ImpressionLogRepository impressionLogRepository;
    private final MediaRepository mediaRepository;

    public AnalyticsService(ImpressionLogRepository impressionLogRepository,
                            MediaRepository mediaRepository) {
        this.impressionLogRepository = impressionLogRepository;
        this.mediaRepository = mediaRepository;
    }

    public AnalyticsResponse queryImpressions(UUID screenId, UUID mediaId,
                                               LocalDate from, LocalDate to) {
        OffsetDateTime fromDt = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDt = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Object[]> rawResults = mediaId != null
                ? impressionLogRepository.aggregateByMediaFiltered(screenId, mediaId, fromDt, toDt)
                : impressionLogRepository.aggregateByMedia(screenId, fromDt, toDt);

        Set<UUID> mediaIds = rawResults.stream()
                .map(row -> (UUID) row[0])
                .collect(Collectors.toSet());

        Map<UUID, String> mediaNames = mediaRepository.findAllById(mediaIds).stream()
                .collect(Collectors.toMap(m -> m.getId(), m -> m.getName()));

        long totalPlays = 0;
        long totalCompletions = 0;
        List<AnalyticsResponse.MediaStats> byMedia = new java.util.ArrayList<>();

        for (Object[] row : rawResults) {
            UUID mId = (UUID) row[0];
            long plays = ((Number) row[1]).longValue();
            long completions = ((Number) row[2]).longValue();
            String name = mediaNames.getOrDefault(mId, "Unknown");
            byMedia.add(new AnalyticsResponse.MediaStats(mId, name, plays, completions));
            totalPlays += plays;
            totalCompletions += completions;
        }

        String completionRate = totalPlays > 0
                ? String.format("%.1f%%", (double) totalCompletions / totalPlays * 100)
                : "0.0%";

        return new AnalyticsResponse(totalPlays, totalCompletions, completionRate, byMedia);
    }
}
