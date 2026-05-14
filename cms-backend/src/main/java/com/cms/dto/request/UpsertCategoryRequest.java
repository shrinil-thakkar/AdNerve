package com.cms.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpsertCategoryRequest(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
