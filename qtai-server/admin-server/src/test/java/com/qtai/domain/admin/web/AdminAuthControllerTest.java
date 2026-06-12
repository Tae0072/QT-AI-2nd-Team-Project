package com.qtai.domain.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.AdminAuthUseCase;
import com.qtai.domain.admin.api.dto.AdminLoginResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link AdminAuthController} 통합 테스트(MockMvc).
 *
 * <p>보안 설정 변경(SecurityConfig의 /api/v1/admin/auth/login·refresh permitAll)을 동반하므로,
 * 웹 계층에서 다음을 검증한다:
 * <ul>
 *   <li>신규 인증 경로는 <b>비인증 200 도달</b>(permitAll) + 표준 envelope 반환</li>
 *   <li>그 외 {@code /api/v1/admin/**}은 여전히 <b>ROLE_ADMIN 강제</b>(401/403)</li>
 *   <li>{@code @NotBlank} 검증 실패 시 400</li>
 *   <li>{@code ADMIN_LOGIN_FAILED} → 401, {@code ADMIN_USER_DISABLED} → 403 매핑</li>
 * </ul>
 * UseCase는 {@code @MockBean}으로 대체해 웹/보안 계층 동작에 집중한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminAuthUseCase adminAuthUseCase;

    private AdminLoginResult sampleResult() {
        return new AdminLoginResult(
                "access-tok", "refresh-tok",
                new AdminLoginResult.Admin(1L, "개발관리자", "ADMIN", "OPERATOR", "ACTIVE"));
    }

    @Test
    @DisplayName("POST /admin/auth/login — 비인증 200(permitAll) + 토큰/요약 반환")
    void login_unauthenticated_returns200() throws Exception {
        when(adminAuthUseCase.login(eq("admin"), eq("admin1234"))).thenReturn(sampleResult());

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-tok"))
                .andExpect(jsonPath("$.data.admin.adminRole").value("OPERATOR"));
    }

    @Test
    @DisplayName("로그인 빈 username → 400(@NotBlank, permitAll 도달 후 검증)")
    void login_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"admin1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자격 불일치 → 401(ADMIN_LOGIN_FAILED)")
    void login_invalidCredentials_returns401() throws Exception {
        when(adminAuthUseCase.login(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비활성 관리자 → 403(ADMIN_USER_DISABLED)")
    void login_disabledAdmin_returns403() throws Exception {
        when(adminAuthUseCase.login(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_USER_DISABLED));

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin1234\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/auth/refresh — 비인증 200(permitAll)")
    void refresh_unauthenticated_returns200() throws Exception {
        when(adminAuthUseCase.refresh(eq("refresh-tok"))).thenReturn(sampleResult());

        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-tok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-tok"));
    }

    @Test
    @DisplayName("permitAll은 auth 경로만 — 다른 /admin/** 비인증 접근은 여전히 401/403")
    void otherAdminPath_stillRequiresAdmin() throws Exception {
        int status = mockMvc.perform(get("/api/v1/admin/me"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}
