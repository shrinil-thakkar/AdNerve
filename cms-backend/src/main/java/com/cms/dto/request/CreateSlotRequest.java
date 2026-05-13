package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSlotRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Pattern(regexp = "time_based|sequential") String slotType,

        // time_based fields
        @Pattern(regexp = "\\d{2}:\\d{2}(:\\d{2})?") String startTime,
        @Pattern(regexp = "\\d{2}:\\d{2}(:\\d{2})?") String endTime,
        List<Integer> daysOfWeek,

        // sequential fields
        Integer sequenceOrder,
        Integer durationSeconds
) {}
