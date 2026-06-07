package com.qtai.domain.member.web;

import com.qtai.domain.member.api.GetSettingsUseCase;
import com.qtai.domain.member.api.UpdateSettingsUseCase;
import com.qtai.domain.member.api.dto.SettingsResponse;
import com.qtai.domain.member.api.dto.SettingsUpdateRequest;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private GetSettingsUseCase getSettingsUseCase;

    @MockBean
    private UpdateSettingsUseCase updateSettingsUseCase;

    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        MEMBER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void GET_설정_조회_정상() throws Exception {
        given(getSettingsUseCase.getSettings(MEMBER_ID))
                .willReturn(new SettingsResponse(true, "MEDIUM", true, 70, "ALL"));

        mockMvc.perform(get("/api/v1/me/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationEnabled").value(true))
                .andExpect(jsonPath("$.data.fontSize").value("MEDIUM"));
    }

    @Test
    void PATCH_설정_수정_정상() throws Exception {
        given(updateSettingsUseCase.updateSettings(eq(MEMBER_ID), any(SettingsUpdateRequest.class)))
                .willReturn(new SettingsResponse(false, "LARGE", true, 70, "ALL"));

        mockMvc.perform(patch("/api/v1/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEnabled\":false,\"fontSize\":\"LARGE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationEnabled").value(false))
                .andExpect(jsonPath("$.data.fontSize").value("LARGE"));
    }

    @Test
    void PATCH_잘못된_fontSize_400() throws Exception {
        mockMvc.perform(patch("/api/v1/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fontSize\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_음악_설정_수정_정상() throws Exception {
        given(updateSettingsUseCase.updateSettings(eq(MEMBER_ID), any(SettingsUpdateRequest.class)))
                .willReturn(new SettingsResponse(true, "MEDIUM", false, 30, "HYMN"));

        mockMvc.perform(patch("/api/v1/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"musicEnabled\":false,\"musicVolume\":30,\"musicCategory\":\"HYMN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.musicEnabled").value(false))
                .andExpect(jsonPath("$.data.musicVolume").value(30))
                .andExpect(jsonPath("$.data.musicCategory").value("HYMN"));
    }

    @Test
    void PATCH_잘못된_musicVolume_400() throws Exception {
        mockMvc.perform(patch("/api/v1/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"musicVolume\":999}"))
                .andExpect(status().isBadRequest());
    }
}
