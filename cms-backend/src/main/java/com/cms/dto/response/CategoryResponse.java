package com.cms.dto.response;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String category,
        String startTime,
        String endTime,
        boolean isActive
) {}
