package com.cms.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEventBus {

    private static final Logger log = LoggerFactory.getLogger(SseEventBus.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String screenId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(screenId, emitter);
        emitter.onCompletion(() -> emitters.remove(screenId));
        emitter.onTimeout(() -> emitters.remove(screenId));
        emitter.onError(e -> emitters.remove(screenId));
        log.info("Screen {} connected to SSE stream. Active connections: {}", screenId, emitters.size());
        return emitter;
    }

    public void publish(String screenId, Object payload) {
        SseEmitter emitter = emitters.get(screenId);
        if (emitter == null) {
            log.warn("No active SSE connection for screen {}", screenId);
            return;
        }
        try {
            emitter.send(
                    SseEmitter.event()
                            .name("schedule_update")
                            .data(payload, MediaType.APPLICATION_JSON)
            );
            log.info("Schedule pushed to screen {}", screenId);
        } catch (IOException e) {
            log.warn("Failed to send SSE event to screen {}, removing emitter", screenId);
            emitters.remove(screenId);
        }
    }

    public boolean isConnected(String screenId) {
        return emitters.containsKey(screenId);
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeatToAll() {
        emitters.forEach((screenId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException e) {
                log.debug("Heartbeat failed for screen {}, removing emitter", screenId);
                emitters.remove(screenId);
            }
        });
    }
}
