package com.qtai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * admin-server 보안·인가 검증 (CLAUDE.md §5: 관리자 API는 ROLE_ADMIN 1차 + admin_users.admin_role 2차).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminServerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("인증 없이 /api/v1/admin/** 접근 시 거부된다(401 또는 403)")
    void unauthenticated_admin_path_is_rejected() throws Exception {
        int status = mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }

    @Test
    @DisplayName("ROLE_ADMIN이 아닌 인증 사용자는 1차 게이트에서 403")
    @WithMockUser(username = "7", roles = "USER")
    void non_admin_role_is_denied_at_filter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(403));
    }

    @Test
    @DisplayName("non ADMIN role is denied for qt-passages by SecurityFilterChain")
    @WithMockUser(username = "7", roles = "USER")
    void non_admin_role_is_denied_for_qt_passages_at_filter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/qt-passages"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(403));
    }

    @Test
    @DisplayName("ROLE_ADMIN이라도 admin_users 미등록이면 2차 admin_role 검증에서 403")
    @WithMockUser(username = "7", roles = "ADMIN")
    void admin_role_without_admin_user_record_is_denied_at_service_layer() throws Exception {
        // 1차 ROLE_ADMIN은 통과하지만, test H2의 admin_users가 비어 있어 2차 검증(VerifyAdminRoleUseCase)이
        // ADMIN_USER_NOT_FOUND(403)로 거부 — 2단계 관리자 인가가 작동함을 보장.
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(403));
    }
}
