package com.qtai.note;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.note.client.qt.NoteQtClient;
import com.qtai.domain.report.client.ai.CheckAiQaRequestExistsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * service-note 컨트롤러 통합 테스트(MockMvc + 실제 SecurityConfig + H2).
 *
 * <p>검증: 미인증 차단(401/403), 인증 시 정상 응답(200/201), 쿼리파라미터(verseId/qtPassageId) 처리,
 * {@code /api/v1/admin/**} denyAll 회귀 방지(403). 인증은 {@code @AuthenticationPrincipal Long memberId}로
 * 해석되도록 principal에 memberId(Long)를 주입한다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
class NoteApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // service-note 자체 통합 테스트는 cross-service HTTP 호출을 타지 않도록 클라이언트를 스텁한다(MSA 격리).
    // 실제 RestClient 어댑터(qt readability·bible 구절 조회)의 호출 자체 검증은 어댑터 단위 테스트가 담당한다.
    @MockBean
    private NoteQtClient noteQtClient;
    @MockBean
    private GetBibleVerseUseCase getBibleVerseUseCase;
    @MockBean
    private CheckAiQaRequestExistsClient checkAiQaRequestExistsClient;

    /** principal = memberId(Long), 권한 ROLE_USER 인 인증 컨텍스트를 주입한다. */
    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @DisplayName("미인증 노트 목록 요청은 401 또는 403으로 차단된다")
    void 미인증_노트목록_차단() throws Exception {
        mockMvc.perform(get("/api/v1/notes"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("인증된 노트 목록 요청은 200과 표준 envelope를 반환한다")
    void 인증_노트목록_200() throws Exception {
        mockMvc.perform(get("/api/v1/notes").with(user(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("묵상 노트 초안 조회는 qtPassageId를 쿼리파라미터로 받아 200을 반환한다(서비스 간 조회 호출 없음)")
    void 인증_묵상초안_쿼리파라미터_200() throws Exception {
        mockMvc.perform(get("/api/v1/notes/draft")
                        .param("category", "MEDITATION")
                        .param("qtPassageId", "1")
                        .with(user(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(false));
    }

    @Test
    @DisplayName("인증된 묵상 노트 생성은 201을 반환한다(저널 이벤트 적재 경로 포함)")
    void 인증_노트생성_201() throws Exception {
        String body = """
                {"category":"MEDITATION","qtPassageId":10,"title":"오늘의 묵상","body":"본문 내용",
                 "status":"DRAFT","visibility":"PRIVATE"}
                """;
        mockMvc.perform(post("/api/v1/notes")
                        .with(user(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("인증된 신고 접수는 201을 반환한다(검수 대상 외 타입은 존재검증 생략)")
    void 인증_신고접수_201() throws Exception {
        String body = """
                {"targetType":"AI_QA_REQUEST","targetId":42,"reason":"FACT_ERROR","detail":"사실 오류"}
                """;
        when(checkAiQaRequestExistsClient.exists(3L, 42L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/reports")
                        .with(user(3L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("미인증 신고 접수는 401 또는 403으로 차단된다")
    void 미인증_신고접수_차단() throws Exception {
        String body = """
                {"targetType":"AI_QA_REQUEST","targetId":42,"reason":"FACT_ERROR"}
                """;
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("인증되어도 /api/v1/admin/** 은 denyAll로 403 (신고 검수는 admin-server 소관)")
    void 인증_관리자경로_denyAll_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports").with(user(1L)))
                .andExpect(status().isForbidden());
    }
}
