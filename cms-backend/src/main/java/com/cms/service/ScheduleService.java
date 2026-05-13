package com.cms.service;

import com.cms.dto.response.ScheduleResponse;
import com.cms.entity.Screen;
import com.cms.entity.Slot;
import com.cms.entity.SlotAssignment;
import com.cms.repository.AssignmentRepository;
import com.cms.repository.ScreenRepository;
import com.cms.repository.SlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {

    private final ScreenRepository screenRepository;
    private final SlotRepository slotRepository;
    private final AssignmentRepository assignmentRepository;
    private final MediaService mediaService;

    public ScheduleService(ScreenRepository screenRepository, SlotRepository slotRepository,
                           AssignmentRepository assignmentRepository, MediaService mediaService) {
        this.screenRepository = screenRepository;
        this.slotRepository = slotRepository;
        this.assignmentRepository = assignmentRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public ScheduleResponse resolveSchedule(UUID screenId) {
        Screen screen = screenRepository.findByIdAndIsActiveTrue(screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));

        List<Slot> slots = slotRepository.findByScreenIdAndIsActiveTrue(screenId);

        List<ScheduleResponse.TimeSlotEntry> timeSlots = new ArrayList<>();
        List<ScheduleResponse.SequentialSlotEntry> sequentialSlots = new ArrayList<>();

        for (Slot slot : slots) {
            List<SlotAssignment> assignments =
                    assignmentRepository.findBySlotIdAndIsActiveTrueOrderByPriority(slot.getId());
            List<ScheduleResponse.MediaEntry> mediaEntries = assignments.stream()
                    .map(this::buildMediaEntry)
                    .toList();

            if ("time_based".equals(slot.getSlotType())) {
                List<Integer> daysOfWeek = slot.getDaysOfWeek() != null
                        ? Arrays.asList(slot.getDaysOfWeek()) : List.of();
                timeSlots.add(new ScheduleResponse.TimeSlotEntry(
                        slot.getId(),
                        slot.getName(),
                        slot.getStartTime() != null ? slot.getStartTime().toString() : null,
                        slot.getEndTime() != null ? slot.getEndTime().toString() : null,
                        daysOfWeek,
                        mediaEntries
                ));
            } else {
                sequentialSlots.add(new ScheduleResponse.SequentialSlotEntry(
                        slot.getId(),
                        slot.getName(),
                        slot.getSequenceOrder(),
                        slot.getDurationSeconds(),
                        mediaEntries
                ));
            }
        }

        return new ScheduleResponse(
                screen.getId(),
                screen.getTimezone(),
                OffsetDateTime.now(),
                timeSlots,
                sequentialSlots
        );
    }

    private ScheduleResponse.MediaEntry buildMediaEntry(SlotAssignment assignment) {
        String presignedUrl = mediaService.generateDownloadUrl(assignment.getMedia().getS3Key());
        return new ScheduleResponse.MediaEntry(
                assignment.getId(),
                assignment.getMedia().getId(),
                assignment.getMedia().getName(),
                assignment.getMedia().getMediaType(),
                presignedUrl,
                assignment.getMedia().getDurationSeconds(),
                assignment.getPriority()
        );
    }
}
