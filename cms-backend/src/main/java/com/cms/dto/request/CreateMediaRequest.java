package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMediaRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 100) String mimeType,
        @Positive Long fileSizeBytes,
        Integer durationSeconds
) {}
