package com.cms.service;

import com.cms.dto.request.CreateScreenRequest;
import com.cms.dto.response.ScreenResponse;
import com.cms.entity.CategoryType;
import com.cms.entity.Screen;
import com.cms.entity.ScreenCategory;
import com.cms.repository.ScreenCategoryRepository;
import com.cms.repository.ScreenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScreenService {

    private static final int ONLINE_THRESHOLD_SECONDS = 60;

    private final ScreenRepository screenRepository;
    private final ScreenCategoryRepository screenCategoryRepository;

    public ScreenService(ScreenRepository screenRepository, ScreenCategoryRepository screenCategoryRepository) {
        this.screenRepository = screenRepository;
        this.screenCategoryRepository = screenCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> findAll() {
        return screenRepository.findAllByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScreenResponse findById(UUID id) {
        Screen screen = screenRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        return toResponse(screen);
    }

    public ScreenResponse create(CreateScreenRequest request) {
        Screen screen = new Screen();
        screen.setName(request.name());
        screen.setLocation(request.location());
        screen.setTimezone(request.timezone() != null ? request.timezone() : "UTC");
        screen.setDeviceToken(UUID.randomUUID().toString());
        if (request.defaultSlotLengthSeconds() != null) {
            screen.setDefaultSlotLengthSeconds(request.defaultSlotLengthSeconds());
        }
        if (request.defaultSlotCount() != null) {
            screen.setDefaultSlotCount(request.defaultSlotCount());
        }
        Screen saved = screenRepository.save(screen);
        createDefaultCategories(saved.getId());
        return toResponse(saved);
    }

    public ScreenResponse update(UUID id, CreateScreenRequest request) {
        Screen screen = screenRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        if (request.name() != null) screen.setName(request.name());
        if (request.location() != null) screen.setLocation(request.location());
        if (request.timezone() != null) screen.setTimezone(request.timezone());
        if (request.defaultSlotLengthSeconds() != null) screen.setDefaultSlotLengthSeconds(request.defaultSlotLengthSeconds());
        if (request.defaultSlotCount() != null) screen.setDefaultSlotCount(request.defaultSlotCount());
        return toResponse(screenRepository.save(screen));
    }

    public void softDelete(UUID id) {
        Screen screen = screenRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        screen.setIsActive(false);
        screenRepository.save(screen);
    }

    public void updateLastSeen(UUID id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        screen.setLastSeenAt(OffsetDateTime.now());
        screenRepository.save(screen);
    }

    private void createDefaultCategories(UUID screenId) {
        record CategoryDef(CategoryType type, LocalTime start, LocalTime end) {}
        List.of(
                new CategoryDef(CategoryType.MORNING, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new CategoryDef(CategoryType.AFTERNOON, LocalTime.of(12, 0), LocalTime.of(17, 0)),
                new CategoryDef(CategoryType.EVENING, LocalTime.of(17, 0), LocalTime.of(22, 0))
        ).forEach(def -> {
            ScreenCategory cat = new ScreenCategory();
            cat.setScreenId(screenId);
            cat.setCategory(def.type());
            cat.setStartTime(def.start());
            cat.setEndTime(def.end());
            screenCategoryRepository.save(cat);
        });
    }

    private ScreenResponse toResponse(Screen screen) {
        boolean online = screen.getLastSeenAt() != null &&
                screen.getLastSeenAt().isAfter(OffsetDateTime.now().minusSeconds(ONLINE_THRESHOLD_SECONDS));
        return new ScreenResponse(
                screen.getId(),
                screen.getName(),
                screen.getLocation(),
                screen.getTimezone(),
                screen.getDefaultSlotLengthSeconds(),
                screen.getDefaultSlotCount(),
                screen.getLastSeenAt(),
                online,
                Boolean.TRUE.equals(screen.getIsActive())
        );
    }
}
