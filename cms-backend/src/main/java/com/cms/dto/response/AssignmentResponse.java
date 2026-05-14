package com.cms.dto.response;

import java.util.UUID;

public record AssignmentResponse(
        Integer position,
        UUID mediaId,
        String name,
        String mediaType
) {}
