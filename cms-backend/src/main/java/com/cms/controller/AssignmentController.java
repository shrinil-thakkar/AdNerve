package com.cms.controller;

import com.cms.dto.request.AssignSlotRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.AssignmentResponse;
import com.cms.entity.CategoryType;
import com.cms.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/screens/{screenId}/categories/{category}/slots")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> list(
            @PathVariable UUID screenId,
            @PathVariable CategoryType category) {
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listSlots(screenId, category)));
    }

    @PutMapping("/{position}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> assign(
            @PathVariable UUID screenId,
            @PathVariable CategoryType category,
            @PathVariable Integer position,
            @Valid @RequestBody AssignSlotRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentService.assign(screenId, category, position, request.mediaId())));
    }

    @DeleteMapping("/{position}")
    public ResponseEntity<ApiResponse<Void>> clear(
            @PathVariable UUID screenId,
            @PathVariable CategoryType category,
            @PathVariable Integer position) {
        assignmentService.clear(screenId, category, position);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
