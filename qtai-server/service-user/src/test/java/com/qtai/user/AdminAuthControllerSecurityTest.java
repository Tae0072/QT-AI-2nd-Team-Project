package com.qtai.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.AdminLoginUseCase;
import com.qtai.domain.member.api.dto.AdminLoginResponse;
import com.qtai.domain.member.client.kakao.KakaoOAuthClient;
import com.qtai.domain.member.internal.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code POST /api/v1/admin/auth/kakao} 라우팅·보안(permitAll)·오류 매핑 통합 테스트.
 *
 * <p>관리자 로그인은 비인증 진입(permitAll)이며, {@code /api/v1/admin/**}의 denyAll보다 먼저 선언돼 우선한다.
 * 실제 로그인 로직은 {@code AdminAuthServiceTest}에서 단위 검증하므로 여기선 {@link AdminLoginUseCase}를
 * @MockBean으로 격리해 컨트롤러·시큐리티 책임만 본다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
class AdminAuthControllerSecurityTest {

    private static final String URI = "/api/v1/admin/auth/kakao";
    private static final String BODY = "{\"kakaoAccessToken\":\"dummy-kakao-token\"}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminLoginUseCase adminLoginUseCase;
    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;
    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("비인증으로 관리자 로그인(permitAll) → 200 + ADMIN 토큰/역할")
    void 관리자로그인_비인증허용_200() throws Exception {
        given(adminLoginUseCase.adminLogin(any())).willReturn(new AdminLoginResponse(
                "acc", "ref",
                new AdminLoginResponse.AdminSummary(12L, "운영자", "ADMIN", "OPERATOR", "ACTIVE")));

        mvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("acc"))
                .andExpect(jsonPath("$.data.admin.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.admin.adminRole").value("OPERATOR"));
    }

    @Test
    @DisplayName("관리자가 아니면 403 + AD0001")
    void 관리자아님_403() throws Exception {
        given(adminLoginUseCase.adminLogin(any()))
                .willThrow(new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));

        mvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AD0001"));
    }

    @Test
    @DisplayName("빈 카카오 토큰은 400(검증 실패)")
    void 빈토큰_400() throws Exception {
        mvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kakaoAccessToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 외 관리자 경로는 여전히 차단 — permitAll이 /admin/auth/kakao로 한정됨(비인증 401/403)")
    void 다른_관리자경로는_차단() throws Exception {
        int statusCode = mvc.perform(get("/api/v1/admin/anything"))
                .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(statusCode).isIn(401, 403);
    }
}
