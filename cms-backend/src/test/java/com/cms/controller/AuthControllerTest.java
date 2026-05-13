package com.cms.controller;

import com.cms.core.JwtUtil;
import com.cms.dto.request.LoginRequest;
import com.cms.dto.response.TokenResponse;
import com.cms.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;

    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    com.cms.security.AdminUserDetailsService adminUserDetailsService;

    @Test
    @WithMockUser
    void login_validCredentials_returnsToken() throws Exception {
        when(authService.login(any())).thenReturn(new TokenResponse("test-jwt", "bearer"));

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@test.com", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("test-jwt"))
                .andExpect(jsonPath("$.data.tokenType").value("bearer"));
    }

    @Test
    @WithMockUser
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bad@test.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @WithMockUser
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
