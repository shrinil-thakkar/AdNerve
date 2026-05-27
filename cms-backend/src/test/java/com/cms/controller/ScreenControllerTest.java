package com.cms.controller;

import com.cms.core.JwtUtil;
import com.cms.dto.request.CreateScreenRequest;
import com.cms.dto.response.ScreenResponse;
import com.cms.service.ScreenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScreenController.class)
class ScreenControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ScreenService screenService;

    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    com.cms.security.AdminUserDetailsService adminUserDetailsService;

    private ScreenResponse sampleScreen() {
        return new ScreenResponse(UUID.randomUUID(), "Lobby Screen", "Main Entrance",
                "Asia/Kolkata", 10, 6, null, false, true, "ABCD2345");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returnScreens() throws Exception {
        when(screenService.findAll()).thenReturn(List.of(sampleScreen()));

        mockMvc.perform(get("/v1/screens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Lobby Screen"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validRequest_returns201() throws Exception {
        when(screenService.create(any())).thenReturn(sampleScreen());

        mockMvc.perform(post("/v1/screens")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateScreenRequest("Lobby Screen", "Main Entrance", "Asia/Kolkata", 10, 6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void list_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/v1/screens"))
                .andExpect(status().is4xxClientError());
    }
}
