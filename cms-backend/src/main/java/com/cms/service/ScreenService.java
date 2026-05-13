package com.cms.service;

import com.cms.dto.request.CreateScreenRequest;
import com.cms.dto.response.ScreenResponse;
import com.cms.entity.Screen;
import com.cms.repository.ScreenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScreenService {

    private static final int ONLINE_THRESHOLD_SECONDS = 60;

    private final ScreenRepository screenRepository;

    public ScreenService(ScreenRepository screenRepository) {
        this.screenRepository = screenRepository;
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
        return toResponse(screenRepository.save(screen));
    }

    public ScreenResponse update(UUID id, CreateScreenRequest request) {
        Screen screen = screenRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found"));
        if (request.name() != null) screen.setName(request.name());
        if (request.location() != null) screen.setLocation(request.location());
        if (request.timezone() != null) screen.setTimezone(request.timezone());
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

    private ScreenResponse toResponse(Screen screen) {
        boolean isOnline = screen.getLastSeenAt() != null &&
                screen.getLastSeenAt().isAfter(OffsetDateTime.now().minusSeconds(ONLINE_THRESHOLD_SECONDS));
        return new ScreenResponse(
                screen.getId(),
                screen.getName(),
                screen.getLocation(),
                screen.getDeviceToken(),
                screen.getTimezone(),
                screen.getIsActive(),
                screen.getLastSeenAt(),
                screen.getCreatedAt(),
                screen.getUpdatedAt(),
                isOnline
        );
    }
}
