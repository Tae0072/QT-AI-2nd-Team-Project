package com.qtai.domain.admin.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.admin.api.VerifyAdminRoleUseCase;
import com.qtai.domain.admin.api.dto.AdminUserInfo;
import com.qtai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AdminController MockMvc 슬라이스 테스트.
 *
 * <p>Security 필터 비활성화 (addFilters=false).
 * SecurityContextHolder에 직접 Authentication 세팅.
 *
 * <p>CLAUDE.md §10 필수 테스트: admin authorization 검증.
 */
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private VerifyAdminRoleUseCase verifyAdminRoleUseCase;

    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(MEMBER_ID, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ─── GET /api/v1/admin/me ───────────────────────

    @Test
    @DisplayName("GET /admin/me — 활성 관리자 조회 성공 200")
    void getMyAdminInfo_200_성공() throws Exception {
        // given
        AdminUserInfo adminInfo = new AdminUserInfo(10L, MEMBER_ID, "OPERATOR");
        when(verifyAdminRoleUseCase.getActiveAdmin(MEMBER_ID)).thenReturn(adminInfo);

        // when & then
        mockMvc.perform(get("/api/v1/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adminUserId").value(10))
                .andExpect(jsonPath("$.data.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.data.adminRole").value("OPERATOR"));
    }

    @Test
    @DisplayName("GET /admin/me — admin_users 미등록 시 403")
    void getMyAdminInfo_403_미등록() throws Exception {
        // given
        when(verifyAdminRoleUseCase.getActiveAdmin(MEMBER_ID))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/admin/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/me — 비활성 관리자 계정 시 403")
    void getMyAdminInfo_403_비활성() throws Exception {
        // given
        when(verifyAdminRoleUseCase.getActiveAdmin(MEMBER_ID))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_USER_DISABLED));

        // when & then
        mockMvc.perform(get("/api/v1/admin/me"))
                .andExpect(status().isForbidden());
    }
}
