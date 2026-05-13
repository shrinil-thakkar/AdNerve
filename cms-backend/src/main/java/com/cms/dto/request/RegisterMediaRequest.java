package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterMediaRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Pattern(regexp = "image|video") String mediaType,
        @NotBlank @Size(max = 1024) String s3Key,
        Long fileSizeBytes,
        Integer durationSeconds,
        @Size(max = 100) String mimeType
) {}
