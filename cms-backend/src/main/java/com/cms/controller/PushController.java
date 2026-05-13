package com.cms.controller;

import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.ScheduleResponse;
import com.cms.service.PushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/screens")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping("/{screenId}/push")
    public ResponseEntity<ApiResponse<ScheduleResponse>> push(@PathVariable UUID screenId) {
        return ResponseEntity.ok(ApiResponse.ok(pushService.push(screenId)));
    }
}
