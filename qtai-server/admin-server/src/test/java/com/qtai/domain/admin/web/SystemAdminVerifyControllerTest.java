package com.qtai.domain.admin.web;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 관리자 검증 시스템 수신 엔드포인트({@code GET /api/v1/system/admin/verify}) MockMvc 통합 테스트.
 *
 * <p>service-user 등 시스템 배치(SYSTEM_BATCH)가 호출하는 admin 검증 경로의 라우팅·보안·오류 매핑을 검증한다.
 * {@code /api/v1/system/**}은 SecurityConfig에서 {@code hasRole("SYSTEM_BATCH")}로 보호된다(일반 사용자·ADMIN은 403, 미인증은 401/403).
 * 검증 로직 자체는 {@link VerifyAdminRoleUseCase}(@MockBean)로 제어해 컨트롤러 책임만 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemAdminVerifyControllerTest {

    private static final String URI = "/api/v1/system/admin/verify";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    @Test
    @DisplayName("SYSTEM_BATCH + requiredRoles 없음 → getActiveAdmin 결과를 200으로 반환한다")
    @WithMockUser(username = "0", roles = "SYSTEM_BATCH")
    void 활성관리자_200() throws Exception {
        given(verifyAdminRoleUseCase.getActiveAdmin(12L))
                .willReturn(new AdminUserInfo(100L, 12L, "SUPER_ADMIN"));

        mockMvc.perform(get(URI).param("memberId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adminRole").value("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("SYSTEM_BATCH + requiredRoles 있음 → verifyAnyRole 결과를 200으로 반환한다")
    @WithMockUser(username = "0", roles = "SYSTEM_BATCH")
    void 역할검증_200() throws Exception {
        given(verifyAdminRoleUseCase.verifyAnyRole(eq(12L), anyCollection()))
                .willReturn(new AdminUserInfo(100L, 12L, "OPERATOR"));

        mockMvc.perform(get(URI).param("memberId", "12").param("requiredRoles", "OPERATOR", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminRole").value("OPERATOR"));
    }

    @Test
    @DisplayName("관리자가 아니면 403 + 표준 에러 코드(AD0001)로 반환한다")
    @WithMockUser(username = "0", roles = "SYSTEM_BATCH")
    void 관리자아님_403() throws Exception {
        given(verifyAdminRoleUseCase.getActiveAdmin(7L))
                .willThrow(new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));

        mockMvc.perform(get(URI).param("memberId", "7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AD0001"));
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 시스템 경로에서 403")
    @WithMockUser(username = "7", roles = "USER")
    void 사용자_403() throws Exception {
        mockMvc.perform(get(URI).param("memberId", "12"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 요청은 401 또는 403으로 차단한다")
    void 미인증_차단() throws Exception {
        int statusCode = mockMvc.perform(get(URI).param("memberId", "12"))
                .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(statusCode).isIn(401, 403);
    }
}
