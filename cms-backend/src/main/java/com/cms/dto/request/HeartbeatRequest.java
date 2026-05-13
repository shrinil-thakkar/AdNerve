package com.cms.dto.request;

import java.time.OffsetDateTime;

public record HeartbeatRequest(
        OffsetDateTime currentScheduleGeneratedAt
) {}
