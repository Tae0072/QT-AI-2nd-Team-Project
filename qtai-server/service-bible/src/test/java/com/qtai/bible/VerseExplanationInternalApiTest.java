package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/**
 * 승인 해설 게시/숨김/조회 내부 엔드포인트(서비스 간 배치 호출용) MockMvc 통합 테스트.
 *
 * <p>service-ai 배치가 호출하는 {@code /api/v1/study/verse-explanations}(POST 게시·GET 조회)와
 * {@code /hide}(POST 숨김)의 라우팅·권한·정상 응답을 고정한다. 게시/숨김은 콘텐츠 상태 변경 쓰기,
 * 조회는 aiAssetId 메타를 포함하므로 <b>SYSTEM_BATCH 전용</b>이다(일반 사용자·ADMIN은 403, §7·§10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VerseExplanationInternalApiTest {

    private static final String BASE = "/api/v1/study/verse-explanations";

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

    private static final String PUBLISH_BODY = """
            {"bibleVerseId":1001,"summary":"요약","explanation":"해설 본문",
             "sourceLabel":"QT-AI DeepSeek","aiAssetId":5001,"approvedAt":"2026-06-10T00:00:00Z"}
            """;

    @Test
    @DisplayName("SYSTEM_BATCH는 승인 해설을 게시(POST)하고 200으로 결과를 받는다")
    void 게시_시스템배치_200() throws Exception {
        mockMvc.perform(post(BASE).with(systemBatch())
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLISH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bibleVerseId").value(1001))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 게시 해설을 숨기고(POST /hide) 200으로 hiddenCount를 받는다")
    void 숨김_시스템배치_200() throws Exception {
        mockMvc.perform(post(BASE + "/hide").with(systemBatch())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aiAssetId\":5001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiAssetId").value(5001))
                .andExpect(jsonPath("$.data.hiddenCount").value(0));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 verseIds로 승인 해설을 조회하고 200(빈 목록)을 받는다")
    void 조회_시스템배치_200() throws Exception {
        mockMvc.perform(get(BASE).param("verseIds", "1001").with(systemBatch()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 게시/조회 모두 403")
    void 사용자_403() throws Exception {
        mockMvc.perform(post(BASE).with(user(123L))
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLISH_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE).param("verseIds", "1001").with(user(123L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 요청은 401")
    void 미인증_401() throws Exception {
        mockMvc.perform(get(BASE).param("verseIds", "1001"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLISH_BODY))
                .andExpect(status().isUnauthorized());
    }
}
