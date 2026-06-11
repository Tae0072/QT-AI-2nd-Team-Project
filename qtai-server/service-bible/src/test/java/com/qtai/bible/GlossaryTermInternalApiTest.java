package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlossaryTermInternalApiTest {

    private static final String BASE = "/api/v1/study/glossary-terms";
    private static final String PUBLISH_BODY = """
            {
              "aiAssetId": 5001,
              "sourceLabel": "QT-AI DeepSeek",
              "approvedAt": "2026-06-10T00:00:00Z",
              "terms": [
                {
                  "bibleVerseId": 1001,
                  "term": "faith",
                  "meaning": "trust in God"
                }
              ]
            }
            """;
    private static final String HIDE_BODY = "{\"aiAssetId\":5001}";

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor systemBatch() {
        return authentication(new UsernamePasswordAuthenticationToken(
                0L, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_BATCH"))));
    }

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static RequestPostProcessor admin(long adminId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                adminId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 glossary terms 게시를 호출할 수 있다")
    void publishWithSystemBatchReturns200() throws Exception {
        mockMvc.perform(post(BASE).with(systemBatch())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiAssetId").value(5001))
                .andExpect(jsonPath("$.data.publishedCount").value(1));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 glossary terms 숨김을 호출할 수 있다")
    void hideWithSystemBatchReturns200() throws Exception {
        mockMvc.perform(post(BASE + "/hide").with(systemBatch())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HIDE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiAssetId").value(5001))
                .andExpect(jsonPath("$.data.hiddenCount").value(0));
    }

    @Test
    @DisplayName("USER와 ADMIN은 glossary terms 게시와 숨김을 호출할 수 없다")
    void userAndAdminAreForbidden() throws Exception {
        mockMvc.perform(post(BASE).with(user(123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).with(admin(999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/hide").with(user(123L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HIDE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/hide").with(admin(999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HIDE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 요청은 glossary terms 게시와 숨김 모두 401이다")
    void unauthenticatedRequestsReturn401() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HIDE_BODY))
                .andExpect(status().isUnauthorized());
    }
}
