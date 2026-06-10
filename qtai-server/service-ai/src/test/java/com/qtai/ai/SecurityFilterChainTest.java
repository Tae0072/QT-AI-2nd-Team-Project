package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * service-ai 보안 필터체인 검증 (CLAUDE.md §5).
 *
 * <ul>
 *   <li>인증되지 않은 요청은 보호 경로에서 401로 거부된다(anyRequest authenticated).</li>
 *   <li>{@code /api/v1/admin/**}는 denyAll이라, ROLE_ADMIN 토큰을 가진 사용자라도 403으로 막힌다.
 *       (관리자 AI 기능은 admin_role 이중검증을 포함해 admin-server 소관 — 콘텐츠/AI 서비스에서
 *       ROLE_ADMIN 단독으로 열리는 우회를 차단한다.)</li>
 * </ul>
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("인증 없이 보호 경로(system) 접근 시 거부된다(401 또는 403)")
    void unauthenticated_protected_path_is_rejected() throws Exception {
        // 운영에서는 JWT entry point가 401을 반환하지만, 테스트 컨텍스트에는 JWT 필터
        // (security.jwt.public-key 미설정)가 없어 익명 거부가 403으로 떨어질 수 있다.
        // 핵심 보장은 "미인증 요청은 통과하지 못한다"이므로 401/403 모두 허용한다.
        int status = mockMvc.perform(get("/api/v1/system/ai/generation-jobs"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }

    @Test
    @DisplayName("ROLE_ADMIN 사용자라도 /api/v1/admin/** 는 denyAll로 403")
    @WithMockUser(roles = "ADMIN")
    void admin_path_is_denied_even_for_admin_role() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/assets"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("일반 인증 사용자가 /api/v1/admin/** 접근 시 403")
    @WithMockUser(roles = "USER")
    void admin_path_is_denied_for_normal_user() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/validation-checklists"))
                .andExpect(status().isForbidden());
    }
}
