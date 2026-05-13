package com.cms.service;

import com.cms.core.SseEventBus;
import com.cms.dto.response.ScheduleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final ScheduleService scheduleService;
    private final SseEventBus sseEventBus;

    public PushService(ScheduleService scheduleService, SseEventBus sseEventBus) {
        this.scheduleService = scheduleService;
        this.sseEventBus = sseEventBus;
    }

    public ScheduleResponse push(UUID screenId) {
        ScheduleResponse schedule = scheduleService.resolveSchedule(screenId);
        sseEventBus.publish(screenId.toString(), schedule);
        boolean delivered = sseEventBus.isConnected(screenId.toString());
        if (!delivered) {
            log.info("Screen {} is not connected via SSE — schedule resolved but not pushed", screenId);
        }
        return schedule;
    }
}
