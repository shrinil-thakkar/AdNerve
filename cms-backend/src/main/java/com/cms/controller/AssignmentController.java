package com.cms.controller;

import com.cms.dto.request.CreateAssignmentRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.AssignmentResponse;
import com.cms.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/slots/{slotId}/assignments")
public class AssignmentController {

    private final SlotService slotService;

    public AssignmentController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> list(@PathVariable UUID slotId) {
        return ResponseEntity.ok(ApiResponse.ok(slotService.getAssignments(slotId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            @PathVariable UUID slotId,
            @Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(slotService.addAssignment(slotId, request)));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID slotId,
            @PathVariable UUID assignmentId) {
        slotService.removeAssignment(slotId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
