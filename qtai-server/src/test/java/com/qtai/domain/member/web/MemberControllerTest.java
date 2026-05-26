package com.qtai.domain.member.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.member.api.ChangeNicknameUseCase;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MemberController MockMvc 슬라이스 테스트.
 *
 * <p>Security 필터 비활성화 (addFilters=false).
 * SecurityContextHolder 에 직접 Authentication 세팅.
 */
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private GetMemberUseCase getMemberUseCase;

    @MockBean
    private UpdateProfileUseCase updateProfileUseCase;

    @MockBean
    private WithdrawUseCase withdrawUseCase;

    @MockBean
    private ChangeNicknameUseCase changeNicknameUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyInfo_200_성공() throws Exception {
        MemberResponse response = new MemberResponse(
                1L, "testUser", "test@test.com", "https://img.test/pic",
                "ACTIVE", "USER", null, null);
        when(getMemberUseCase.getMember(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("testUser"));
    }

    @Test
    void getMember_공개_프로필_200() throws Exception {
        MemberPublicResponse response = new MemberPublicResponse(2L, "other", "https://img.test/other");
        when(getMemberUseCase.getMemberPublic(2L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/members/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("other"));
    }

    @Test
    void updateProfile_200_성공() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest("newNick", null);
        MemberResponse response = new MemberResponse(
                1L, "newNick", "test@test.com", "https://img.test/pic",
                "ACTIVE", "USER", null, null);
        when(updateProfileUseCase.updateProfile(1L, request)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("newNick"));
    }

    @Test
    void changeNickname_200_성공() throws Exception {
        NicknameChangeRequest request = new NicknameChangeRequest("brandNew");
        MemberResponse response = new MemberResponse(
                1L, "brandNew", "test@test.com", "https://img.test/pic",
                "ACTIVE", "USER", null, null);
        when(changeNicknameUseCase.changeNickname(1L, request)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("brandNew"));
    }

    @Test
    void checkNicknameAvailable_200_true() throws Exception {
        when(changeNicknameUseCase.isNicknameAvailable("unique")).thenReturn(true);

        mockMvc.perform(get("/api/v1/me/nickname/available")
                        .param("nickname", "unique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void withdraw_204_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/me"))
                .andExpect(status().isNoContent());
    }
}
