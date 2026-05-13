package com.cms.controller;

import com.cms.dto.request.CreateScreenRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.ScreenResponse;
import com.cms.service.ScreenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/screens")
public class ScreenController {

    private final ScreenService screenService;

    public ScreenController(ScreenService screenService) {
        this.screenService = screenService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(screenService.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScreenResponse>> create(@Valid @RequestBody CreateScreenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(screenService.create(request)));
    }

    @GetMapping("/{screenId}")
    public ResponseEntity<ApiResponse<ScreenResponse>> get(@PathVariable UUID screenId) {
        return ResponseEntity.ok(ApiResponse.ok(screenService.findById(screenId)));
    }

    @PutMapping("/{screenId}")
    public ResponseEntity<ApiResponse<ScreenResponse>> update(
            @PathVariable UUID screenId,
            @Valid @RequestBody CreateScreenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(screenService.update(screenId, request)));
    }

    @DeleteMapping("/{screenId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID screenId) {
        screenService.softDelete(screenId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
