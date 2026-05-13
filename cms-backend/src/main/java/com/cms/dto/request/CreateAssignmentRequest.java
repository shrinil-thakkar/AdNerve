package com.cms.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID mediaId,
        Integer priority
) {}
