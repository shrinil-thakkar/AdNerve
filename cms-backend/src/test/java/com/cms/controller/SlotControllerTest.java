package com.cms.controller;

import com.cms.core.JwtUtil;
import com.cms.dto.request.CreateSlotRequest;
import com.cms.dto.response.SlotResponse;
import com.cms.service.SlotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SlotController.class)
class SlotControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SlotService slotService;

    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    com.cms.security.AdminUserDetailsService adminUserDetailsService;

    private SlotResponse sampleTimeSlot(UUID screenId) {
        return new SlotResponse(UUID.randomUUID(), screenId, "Morning Prime", "time_based",
                "09:00", "12:00", List.of(0, 1, 2, 3, 4), null, null, true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private SlotResponse sampleSequentialSlot(UUID screenId) {
        return new SlotResponse(UUID.randomUUID(), screenId, "Playlist Item", "sequential",
                null, null, null, 1, 15, true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTimeBased_returns201() throws Exception {
        UUID screenId = UUID.randomUUID();
        when(slotService.create(eq(screenId), any())).thenReturn(sampleTimeSlot(screenId));

        CreateSlotRequest request = new CreateSlotRequest(
                "Morning Prime", "time_based",
                "09:00:00", "12:00:00", List.of(0, 1, 2, 3, 4),
                null, null);

        mockMvc.perform(post("/v1/screens/{screenId}/slots", screenId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slotType").value("time_based"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSequential_returns201() throws Exception {
        UUID screenId = UUID.randomUUID();
        when(slotService.create(eq(screenId), any())).thenReturn(sampleSequentialSlot(screenId));

        CreateSlotRequest request = new CreateSlotRequest(
                "Playlist Item", "sequential",
                null, null, null, 1, 15);

        mockMvc.perform(post("/v1/screens/{screenId}/slots", screenId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slotType").value("sequential"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listSlots_returns200() throws Exception {
        UUID screenId = UUID.randomUUID();
        when(slotService.findByScreenId(screenId)).thenReturn(List.of(sampleTimeSlot(screenId)));

        mockMvc.perform(get("/v1/screens/{screenId}/slots", screenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
