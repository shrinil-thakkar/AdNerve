package com.cms.controller;

import com.cms.dto.request.HeartbeatRequest;
import com.cms.dto.request.ImpressionBatchRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.core.SseEventBus;
import com.cms.entity.ImpressionLog;
import com.cms.repository.ImpressionLogRepository;
import com.cms.security.DeviceTokenValidator;
import com.cms.service.ScreenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/device")
public class DeviceController {

    private final DeviceTokenValidator tokenValidator;
    private final SseEventBus sseEventBus;
    private final ScreenService screenService;
    private final ImpressionLogRepository impressionLogRepository;

    public DeviceController(DeviceTokenValidator tokenValidator, SseEventBus sseEventBus,
                            ScreenService screenService,
                            ImpressionLogRepository impressionLogRepository) {
        this.tokenValidator = tokenValidator;
        this.sseEventBus = sseEventBus;
        this.screenService = screenService;
        this.impressionLogRepository = impressionLogRepository;
    }

    @GetMapping("/stream")
    public SseEmitter stream(HttpServletRequest request) {
        UUID screenId = tokenValidator.validateAndExtractScreenId(request);
        return sseEventBus.subscribe(screenId.toString());
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            HttpServletRequest request,
            @RequestBody(required = false) HeartbeatRequest body) {
        UUID screenId = tokenValidator.validateAndExtractScreenId(request);
        screenService.updateLastSeen(screenId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/impressions")
    public ResponseEntity<ApiResponse<Void>> impressions(
            HttpServletRequest request,
            @Valid @RequestBody ImpressionBatchRequest body) {
        UUID screenId = tokenValidator.validateAndExtractScreenId(request);
        List<ImpressionLog> logs = body.impressions().stream()
                .map(entry -> {
                    ImpressionLog log = new ImpressionLog();
                    log.setScreenId(screenId);
                    log.setMediaId(entry.mediaId());
                    log.setCategory(entry.category());
                    log.setSlotPosition(entry.slotPosition());
                    log.setPlayedAt(entry.playedAt());
                    log.setDurationPlayedSeconds(entry.durationPlayedSeconds());
                    log.setCompleted(Boolean.TRUE.equals(entry.completed()));
                    return log;
                })
                .toList();
        impressionLogRepository.saveAll(logs);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
