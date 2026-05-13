package com.cms.service;

import com.cms.dto.response.ScheduleResponse;
import com.cms.entity.Screen;
import com.cms.entity.Slot;
import com.cms.repository.AssignmentRepository;
import com.cms.repository.ScreenRepository;
import com.cms.repository.SlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScreenRepository screenRepository;
    @Mock SlotRepository slotRepository;
    @Mock AssignmentRepository assignmentRepository;
    @Mock MediaService mediaService;

    @InjectMocks
    ScheduleService scheduleService;

    @Test
    void resolveSchedule_screenNotFound_throws404() {
        UUID screenId = UUID.randomUUID();
        when(screenRepository.findByIdAndIsActiveTrue(screenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.resolveSchedule(screenId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Screen not found");
    }

    @Test
    void resolveSchedule_noSlots_returnsEmptySchedule() {
        UUID screenId = UUID.randomUUID();
        Screen screen = new Screen();
        screen.setId(screenId);
        screen.setTimezone("UTC");

        when(screenRepository.findByIdAndIsActiveTrue(screenId)).thenReturn(Optional.of(screen));
        when(slotRepository.findByScreenIdAndIsActiveTrue(screenId)).thenReturn(List.of());

        ScheduleResponse schedule = scheduleService.resolveSchedule(screenId);

        assertThat(schedule.screenId()).isEqualTo(screenId);
        assertThat(schedule.timeSlots()).isEmpty();
        assertThat(schedule.sequentialSlots()).isEmpty();
    }

    @Test
    void resolveSchedule_withTimeBasedSlot_buildsCorrectResponse() {
        UUID screenId = UUID.randomUUID();
        Screen screen = new Screen();
        screen.setId(screenId);
        screen.setTimezone("Asia/Kolkata");

        Slot slot = new Slot();
        slot.setId(UUID.randomUUID());
        slot.setScreen(screen);
        slot.setSlotType("time_based");
        slot.setName("Morning");

        when(screenRepository.findByIdAndIsActiveTrue(screenId)).thenReturn(Optional.of(screen));
        when(slotRepository.findByScreenIdAndIsActiveTrue(screenId)).thenReturn(List.of(slot));
        when(assignmentRepository.findBySlotIdAndIsActiveTrueOrderByPriority(slot.getId()))
                .thenReturn(List.of());

        ScheduleResponse schedule = scheduleService.resolveSchedule(screenId);

        assertThat(schedule.timeSlots()).hasSize(1);
        assertThat(schedule.timeSlots().get(0).name()).isEqualTo("Morning");
        assertThat(schedule.sequentialSlots()).isEmpty();
    }
}
