package com.qtai.domain.audit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시스템 감사 기록 수신 엔드포인트({@code POST /api/v1/system/audit-logs}) MockMvc 통합 테스트.
 *
 * <p>service-ai 등 시스템 배치가 호출하는 감사 기록 경로의 라우팅·권한·정상 응답을 고정한다.
 * {@code /api/v1/system/**}은 SecurityConfig에서 {@code hasRole("SYSTEM_BATCH")}로 보호된다
 * (일반 사용자·ADMIN은 403, 미인증은 401/403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SystemAuditLogControllerTest {

    private static final String URI = "/api/v1/system/audit-logs";
    private static final String BODY = """
            {"adminUserId":null,"actorType":"SYSTEM","actorId":0,"actorLabel":"AI",
             "actionType":"AI_ASSET_APPROVE","targetType":"AI_ASSET","targetId":5001,
             "beforeJson":null,"afterJson":"{}"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SYSTEM_BATCH는 감사 로그 기록을 200으로 받는다")
    @WithMockUser(username = "0", roles = "SYSTEM_BATCH")
    void 시스템배치_기록_200() throws Exception {
        mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 시스템 경로에서 403")
    @WithMockUser(username = "7", roles = "USER")
    void 사용자_403() throws Exception {
        mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 요청은 401 또는 403으로 차단된다")
    void 미인증_차단() throws Exception {
        int statusCode = mockMvc.perform(post(URI).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andReturn().getResponse().getStatus();
        assertThat(statusCode).isIn(401, 403);
    }
}
