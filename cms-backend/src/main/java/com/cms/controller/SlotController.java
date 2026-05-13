package com.cms.controller;

import com.cms.dto.request.CreateSlotRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.SlotResponse;
import com.cms.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/screens/{screenId}/slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SlotResponse>>> list(@PathVariable UUID screenId) {
        return ResponseEntity.ok(ApiResponse.ok(slotService.findByScreenId(screenId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SlotResponse>> create(
            @PathVariable UUID screenId,
            @Valid @RequestBody CreateSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(slotService.create(screenId, request)));
    }

    @PutMapping("/{slotId}")
    public ResponseEntity<ApiResponse<SlotResponse>> update(
            @PathVariable UUID screenId,
            @PathVariable UUID slotId,
            @Valid @RequestBody CreateSlotRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(slotService.update(screenId, slotId, request)));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID screenId,
            @PathVariable UUID slotId) {
        slotService.delete(screenId, slotId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
