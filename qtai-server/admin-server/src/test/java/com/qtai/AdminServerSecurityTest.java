package com.qtai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * admin-server 보안 검증 — 관리자 경로는 인증 없이 접근할 수 없다(CLAUDE.md §5).
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
}
