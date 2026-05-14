package com.cms.controller;

import com.cms.dto.request.CreateMediaRequest;
import com.cms.dto.response.ApiResponse;
import com.cms.dto.response.CreateMediaResponse;
import com.cms.dto.response.MediaResponse;
import com.cms.security.AdminUserDetails;
import com.cms.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(mediaService.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateMediaResponse>> create(
            @Valid @RequestBody CreateMediaRequest request,
            @AuthenticationPrincipal AdminUserDetails userDetails) {
        UUID userId = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mediaService.create(request, userId)));
    }

    @PatchMapping("/{mediaId}/confirm")
    public ResponseEntity<ApiResponse<MediaResponse>> confirm(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(ApiResponse.ok(mediaService.confirm(mediaId)));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<MediaResponse>> get(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(ApiResponse.ok(mediaService.findById(mediaId)));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID mediaId) {
        mediaService.delete(mediaId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
