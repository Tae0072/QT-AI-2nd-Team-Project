package com.qtai.domain.member.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.LoginUseCase;
import com.qtai.domain.member.api.LogoutUseCase;
import com.qtai.domain.member.api.RefreshTokenUseCase;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.api.dto.RefreshTokenRequest;
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
 * AuthController MockMvc 슬라이스 테스트.
 *
 * <p>Security 필터 비활성화 (addFilters=false).
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private LogoutUseCase logoutUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ── 카카오 로그인 ──

    @Test
    void kakaoLogin_200_정상_로그인() throws Exception {
        LoginResponse response = new LoginResponse(
                "access-jwt", "refresh-jwt",
                new LoginResponse.MemberSummary(1L, "테스트유저", "USER", "ACTIVE", false));

        when(loginUseCase.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("kakao-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.data.member.id").value(1));
    }

    @Test
    void kakaoLogin_401_카카오_인증_실패() throws Exception {
        when(loginUseCase.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.KAKAO_AUTH_FAILED));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kakaoLogin_400_빈_토큰_Validation_실패() throws Exception {
        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(""))))
                .andExpect(status().isBadRequest());

        verify(loginUseCase, never()).login(any());
    }

    // ── 토큰 갱신 ──

    @Test
    void refresh_200_정상_갱신() throws Exception {
        LoginResponse response = new LoginResponse(
                "new-access", "new-refresh",
                new LoginResponse.MemberSummary(1L, "테스트유저", "USER", "ACTIVE", false));

        when(refreshTokenUseCase.refresh(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void refresh_401_유효하지_않은_토큰() throws Exception {
        when(refreshTokenUseCase.refresh(any(RefreshTokenRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("bad"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_400_빈_토큰_Validation_실패() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(""))))
                .andExpect(status().isBadRequest());

        verify(refreshTokenUseCase, never()).refresh(any());
    }

    // ── 로그아웃 ──

    @Test
    void logout_204_정상_로그아웃() throws Exception {
        doNothing().when(logoutUseCase).logout(1L);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
