package com.cms.service;

import com.cms.dto.response.ScheduleResponse;
import com.cms.entity.Screen;
import com.cms.entity.ScreenCategory;
import com.cms.entity.CategoryType;
import com.cms.repository.MediaRepository;
import com.cms.repository.ScreenCategoryRepository;
import com.cms.repository.ScreenRepository;
import com.cms.repository.SlotAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScreenRepository screenRepository;
    @Mock ScreenCategoryRepository screenCategoryRepository;
    @Mock SlotAssignmentRepository slotAssignmentRepository;
    @Mock MediaRepository mediaRepository;
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
    void resolveSchedule_noCategories_returnsEmptySchedule() {
        UUID screenId = UUID.randomUUID();
        Screen screen = new Screen();
        screen.setId(screenId);
        screen.setTimezone("UTC");

        when(screenRepository.findByIdAndIsActiveTrue(screenId)).thenReturn(Optional.of(screen));
        when(screenCategoryRepository.findByScreenIdOrderByStartTime(screenId)).thenReturn(List.of());

        ScheduleResponse schedule = scheduleService.resolveSchedule(screenId);

        assertThat(schedule.screenId()).isEqualTo(screenId);
        assertThat(schedule.categories()).isEmpty();
    }

    @Test
    void resolveSchedule_withCategory_buildsCorrectResponse() {
        UUID screenId = UUID.randomUUID();
        Screen screen = new Screen();
        screen.setId(screenId);
        screen.setTimezone("Asia/Kolkata");

        ScreenCategory sc = new ScreenCategory();
        sc.setId(UUID.randomUUID());
        sc.setScreenId(screenId);
        sc.setCategory(CategoryType.MORNING);
        sc.setStartTime(LocalTime.of(9, 0));
        sc.setEndTime(LocalTime.of(12, 0));
        sc.setIsActive(true);

        when(screenRepository.findByIdAndIsActiveTrue(screenId)).thenReturn(Optional.of(screen));
        when(screenCategoryRepository.findByScreenIdOrderByStartTime(screenId)).thenReturn(List.of(sc));
        when(slotAssignmentRepository.findByScreenCategoryIdOrderBySlotPosition(sc.getId())).thenReturn(List.of());

        ScheduleResponse schedule = scheduleService.resolveSchedule(screenId);

        assertThat(schedule.categories()).hasSize(1);
        assertThat(schedule.categories().get(0).category()).isEqualTo("MORNING");
        assertThat(schedule.categories().get(0).slots()).hasSize(screen.getDefaultSlotCount());
    }
}
