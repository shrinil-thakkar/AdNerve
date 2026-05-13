package com.cms.service;

import com.cms.dto.request.CreateAssignmentRequest;
import com.cms.dto.request.CreateSlotRequest;
import com.cms.dto.response.AssignmentResponse;
import com.cms.dto.response.SlotResponse;
import com.cms.entity.Media;
import com.cms.entity.Screen;
import com.cms.entity.Slot;
import com.cms.entity.SlotAssignment;
import com.cms.repository.AssignmentRepository;
import com.cms.repository.MediaRepository;
import com.cms.repository.ScreenRepository;
import com.cms.repository.SlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SlotService {

    private final SlotRepository slotRepository;
    private final ScreenRepository screenRepository;
    private final AssignmentRepository assignmentRepository;
    private final MediaRepository mediaRepository;

    public SlotService(SlotRepository slotRepository, ScreenRepository screenRepository,
                       AssignmentRepository assignmentRepository, MediaRepository mediaRepository) {
        this.slotRepository = slotRepository;
        this.screenRepository = screenRepository;
        this.assignmentRepository = assignmentRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> findByScreenId(UUID screenId) {
        requireActiveScreen(screenId);
        return slotRepository.findByScreenIdAndIsActiveTrue(screenId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SlotResponse create(UUID screenId, CreateSlotRequest request) {
        Screen screen = requireActiveScreen(screenId);
        Slot slot = new Slot();
        slot.setScreen(screen);
        populateSlot(slot, request);
        return toResponse(slotRepository.save(slot));
    }

    public SlotResponse update(UUID screenId, UUID slotId, CreateSlotRequest request) {
        requireActiveScreen(screenId);
        Slot slot = slotRepository.findByIdAndScreenIdAndIsActiveTrue(slotId, screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        populateSlot(slot, request);
        return toResponse(slotRepository.save(slot));
    }

    public void delete(UUID screenId, UUID slotId) {
        requireActiveScreen(screenId);
        Slot slot = slotRepository.findByIdAndScreenIdAndIsActiveTrue(slotId, screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        slot.setIsActive(false);
        slotRepository.save(slot);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(UUID slotId) {
        requireActiveSlot(slotId);
        return assignmentRepository.findBySlotIdAndIsActiveTrueOrderByPriority(slotId).stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    public AssignmentResponse addAssignment(UUID slotId, CreateAssignmentRequest request) {
        Slot slot = requireActiveSlot(slotId);
        Media media = mediaRepository.findById(request.mediaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));

        if (assignmentRepository.existsBySlotIdAndMediaIdAndIsActiveTrue(slotId, request.mediaId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Media already assigned to this slot");
        }

        SlotAssignment assignment = new SlotAssignment();
        assignment.setSlot(slot);
        assignment.setMedia(media);
        assignment.setPriority(request.priority() != null ? request.priority() : 0);
        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    public void removeAssignment(UUID slotId, UUID assignmentId) {
        requireActiveSlot(slotId);
        SlotAssignment assignment = assignmentRepository.findByIdAndSlotId(assignmentId, slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        assignmentRepository.delete(assignment);
    }

    private Screen requireActiveScreen(UUID screenId) {
        return screenRepository.findByIdAndIsActiveTrue(screenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
    }

    private Slot requireActiveSlot(UUID slotId) {
        return slotRepository.findById(slotId)
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
    }

    private void populateSlot(Slot slot, CreateSlotRequest request) {
        if (request.name() != null) slot.setName(request.name());
        if (request.slotType() != null) slot.setSlotType(request.slotType());
        if (request.startTime() != null) slot.setStartTime(LocalTime.parse(request.startTime()));
        if (request.endTime() != null) slot.setEndTime(LocalTime.parse(request.endTime()));
        if (request.daysOfWeek() != null) slot.setDaysOfWeek(request.daysOfWeek().toArray(new Integer[0]));
        if (request.sequenceOrder() != null) slot.setSequenceOrder(request.sequenceOrder());
        if (request.durationSeconds() != null) slot.setDurationSeconds(request.durationSeconds());
    }

    private SlotResponse toResponse(Slot slot) {
        List<Integer> daysOfWeek = slot.getDaysOfWeek() != null
                ? Arrays.asList(slot.getDaysOfWeek()) : null;
        return new SlotResponse(
                slot.getId(),
                slot.getScreen().getId(),
                slot.getName(),
                slot.getSlotType(),
                slot.getStartTime() != null ? slot.getStartTime().toString() : null,
                slot.getEndTime() != null ? slot.getEndTime().toString() : null,
                daysOfWeek,
                slot.getSequenceOrder(),
                slot.getDurationSeconds(),
                slot.getIsActive(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }

    private AssignmentResponse toAssignmentResponse(SlotAssignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getSlot().getId(),
                a.getMedia().getId(),
                a.getMedia().getName(),
                a.getMedia().getMediaType(),
                a.getPriority(),
                a.getIsActive(),
                a.getAssignedAt()
        );
    }
}
