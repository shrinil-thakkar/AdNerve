package com.cms.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID slotId,
        UUID mediaId,
        String mediaName,
        String mediaType,
        Integer priority,
        Boolean isActive,
        OffsetDateTime assignedAt
) {}
