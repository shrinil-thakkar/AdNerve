package com.cms.service;

import com.cms.core.SseEventBus;
import com.cms.dto.response.ScheduleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    @Mock ScheduleService scheduleService;
    @Mock SseEventBus sseEventBus;

    @InjectMocks
    PushService pushService;

    @Test
    void push_publishesToSseAndReturnsSchedule() {
        UUID screenId = UUID.randomUUID();
        ScheduleResponse schedule = new ScheduleResponse(
                screenId, "UTC", OffsetDateTime.now(), List.of(), List.of());

        when(scheduleService.resolveSchedule(screenId)).thenReturn(schedule);
        when(sseEventBus.isConnected(screenId.toString())).thenReturn(true);

        ScheduleResponse result = pushService.push(screenId);

        assertThat(result).isEqualTo(schedule);
        verify(sseEventBus).publish(screenId.toString(), schedule);
    }

    @Test
    void push_noSseConnection_stillReturnsSchedule() {
        UUID screenId = UUID.randomUUID();
        ScheduleResponse schedule = new ScheduleResponse(
                screenId, "UTC", OffsetDateTime.now(), List.of(), List.of());

        when(scheduleService.resolveSchedule(screenId)).thenReturn(schedule);
        when(sseEventBus.isConnected(screenId.toString())).thenReturn(false);

        ScheduleResponse result = pushService.push(screenId);

        assertThat(result).isEqualTo(schedule);
        verify(sseEventBus).publish(screenId.toString(), schedule);
    }
}
