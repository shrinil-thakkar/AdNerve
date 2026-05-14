package com.cms.service;

import com.cms.dto.response.ScheduleResponse;
import com.cms.entity.Media;
import com.cms.entity.Screen;
import com.cms.entity.ScreenCategory;
import com.cms.entity.SlotAssignment;
import com.cms.repository.MediaRepository;
import com.cms.repository.ScreenCategoryRepository;
import com.cms.repository.ScreenRepository;
import com.cms.repository.SlotAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScreenRepository screenRepository;
    private final ScreenCategoryRepository screenCategoryRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;

    public ScheduleResponse resolveSchedule(UUID screenId) {
        Screen screen = screenRepository.findByIdAndIsActiveTrue(screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));

        List<ScreenCategory> categories =
                screenCategoryRepository.findByScreenIdOrderByStartTime(screenId);

        List<ScheduleResponse.CategoryEntry> categoryEntries = categories.stream()
                .map(sc -> buildCategoryEntry(sc, screen.getDefaultSlotCount()))
                .toList();

        return new ScheduleResponse(
                screen.getId(),
                screen.getTimezone(),
                screen.getDefaultSlotLengthSeconds(),
                OffsetDateTime.now(),
                categoryEntries
        );
    }

    private ScheduleResponse.CategoryEntry buildCategoryEntry(ScreenCategory sc, int slotCount) {
        List<SlotAssignment> assignments =
                slotAssignmentRepository.findByScreenCategoryIdOrderBySlotPosition(sc.getId());
        Map<Integer, SlotAssignment> byPosition = assignments.stream()
                .collect(Collectors.toMap(SlotAssignment::getSlotPosition, a -> a));

        List<ScheduleResponse.SlotEntry> slots = IntStream.rangeClosed(1, slotCount)
                .mapToObj(pos -> {
                    SlotAssignment a = byPosition.get(pos);
                    if (a == null) return new ScheduleResponse.SlotEntry(pos, null, null, null, null);
                    Media media = mediaRepository.findById(a.getMediaId()).orElse(null);
                    if (media == null || !MediaService.STATUS_ACTIVE.equals(media.getStatus())) {
                        return new ScheduleResponse.SlotEntry(pos, null, null, null, null);
                    }
                    String url = mediaService.generateDownloadUrl(media.getS3Key());
                    return new ScheduleResponse.SlotEntry(
                            pos, media.getId(), media.getName(), media.getMediaType(), url);
                })
                .toList();

        return new ScheduleResponse.CategoryEntry(
                sc.getCategory().name(),
                sc.getStartTime().toString(),
                sc.getEndTime().toString(),
                Boolean.TRUE.equals(sc.getIsActive()),
                slots
        );
    }
}
