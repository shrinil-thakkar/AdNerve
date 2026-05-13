package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateScreenRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String location,
        @Size(max = 64) String timezone
) {}
