package com.qtai.domain.member.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.member.api.LoginWithKakaoCodeUseCase;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KakaoWebAuthController MockMvc 슬라이스 테스트 (웹 카카오 로그인, B안 · DRAFT).
 */
@WebMvcTest(KakaoWebAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class KakaoWebAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private LoginWithKakaoCodeUseCase loginWithKakaoCodeUseCase;

    @Test
    void kakaoWebLogin_200_인가코드로_JWT_발급() throws Exception {
        given(loginWithKakaoCodeUseCase.loginWithKakaoCode(any())).willReturn(
                new LoginResponse("acc", "ref",
                        new LoginResponse.MemberSummary(1L, "닉네임", "USER", "ACTIVE", false)));

        mockMvc.perform(post("/api/v1/auth/kakao/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"auth-code-abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }

    @Test
    void kakaoWebLogin_코드_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/kakao/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
