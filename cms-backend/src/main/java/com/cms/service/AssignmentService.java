package com.cms.service;

import com.cms.dto.response.AssignmentResponse;
import com.cms.entity.CategoryType;
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
@Transactional
public class AssignmentService {

    private final ScreenRepository screenRepository;
    private final ScreenCategoryRepository screenCategoryRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;
    private final MediaRepository mediaRepository;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listSlots(UUID screenId, CategoryType category) {
        Screen screen = requireScreen(screenId);
        ScreenCategory sc = requireCategory(screenId, category);
        List<SlotAssignment> assignments =
                slotAssignmentRepository.findByScreenCategoryIdOrderBySlotPosition(sc.getId());
        Map<Integer, SlotAssignment> byPosition = assignments.stream()
                .collect(Collectors.toMap(SlotAssignment::getSlotPosition, a -> a));
        return IntStream.rangeClosed(1, screen.getDefaultSlotCount())
                .mapToObj(pos -> {
                    SlotAssignment a = byPosition.get(pos);
                    if (a == null) return new AssignmentResponse(pos, null, null, null);
                    Media media = mediaRepository.findById(a.getMediaId()).orElse(null);
                    if (media == null) return new AssignmentResponse(pos, null, null, null);
                    return new AssignmentResponse(pos, media.getId(), media.getName(), media.getMediaType());
                })
                .toList();
    }

    public AssignmentResponse assign(UUID screenId, CategoryType category, Integer position, UUID mediaId) {
        Screen screen = requireScreen(screenId);
        if (position < 1 || position > screen.getDefaultSlotCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Position must be between 1 and " + screen.getDefaultSlotCount());
        }
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        if (!MediaService.STATUS_ACTIVE.equals(media.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media is not active");
        }
        if (!"video".equals(media.getMediaType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only video media can be assigned to slots");
        }
        if (media.getDurationSeconds() != null
                && !media.getDurationSeconds().equals(screen.getDefaultSlotLengthSeconds())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Video duration " + media.getDurationSeconds() + "s does not match slot length "
                            + screen.getDefaultSlotLengthSeconds() + "s");
        }
        ScreenCategory sc = requireCategory(screenId, category);
        SlotAssignment assignment = slotAssignmentRepository
                .findByScreenCategoryIdAndSlotPosition(sc.getId(), position)
                .orElseGet(() -> {
                    SlotAssignment a = new SlotAssignment();
                    a.setScreenCategoryId(sc.getId());
                    a.setSlotPosition(position);
                    a.setAssignedAt(OffsetDateTime.now());
                    return a;
                });
        assignment.setMediaId(mediaId);
        assignment.setUpdatedAt(OffsetDateTime.now());
        slotAssignmentRepository.save(assignment);
        return new AssignmentResponse(position, media.getId(), media.getName(), media.getMediaType());
    }

    public void clear(UUID screenId, CategoryType category, Integer position) {
        Screen screen = requireScreen(screenId);
        if (position < 1 || position > screen.getDefaultSlotCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Position must be between 1 and " + screen.getDefaultSlotCount());
        }
        ScreenCategory sc = requireCategory(screenId, category);
        slotAssignmentRepository.deleteByScreenCategoryIdAndSlotPosition(sc.getId(), position);
    }

    private Screen requireScreen(UUID screenId) {
        return screenRepository.findByIdAndIsActiveTrue(screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
    }

    private ScreenCategory requireCategory(UUID screenId, CategoryType category) {
        return screenCategoryRepository.findByScreenIdAndCategory(screenId, category)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category " + category + " not found for screen"));
    }
}
