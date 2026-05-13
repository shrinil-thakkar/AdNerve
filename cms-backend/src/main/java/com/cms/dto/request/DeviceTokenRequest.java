package com.cms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeviceTokenRequest(
        @NotNull UUID screenId,
        @Min(1) @Max(3650) int expiresDays
) {}
