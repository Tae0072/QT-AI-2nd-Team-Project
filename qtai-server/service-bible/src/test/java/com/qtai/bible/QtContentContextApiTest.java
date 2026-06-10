package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.qt.internal.QtPassage;
import com.qtai.domain.qt.internal.QtPassageRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * QT 콘텐츠 컨텍스트 내부 엔드포인트(서비스 간 배치 호출용) MockMvc 통합 테스트.
 *
 * <p>service-ai 배치가 호출하는 {@code GET /api/v1/qt/passages/{id}/content-context}와
 * {@code GET /api/v1/qt/content-context?qtDate=}의 라우팅·권한·정상/404를 고정한다.
 * 이 엔드포인트는 미공개 본문도 반환하므로 <b>SYSTEM_BATCH 전용</b>이다(일반 사용자·ADMIN은 403).
 * 인증은 실제 JWT 발급 없이 SecurityContext에 권한을 주입한다(실서버 필터와 동일 형태).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QtContentContextApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QtPassageRepository qtPassageRepository;

    private static RequestPostProcessor systemBatch() {
        return authentication(new UsernamePasswordAuthenticationToken(
                0L, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_BATCH"))));
    }

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /** 공개 게이트상 published=true가 되도록 과거(어제) 날짜로 본문을 시드한다. */
    private QtPassage seedPassage(LocalDate date) {
        return qtPassageRepository.save(
                QtPassage.create(date, (short) 19, (short) 23, (short) 1, (short) 6, "제목", "시 23:1-6"));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 id로 콘텐츠 컨텍스트를 200으로 받는다")
    void id_조회_시스템배치_200() throws Exception {
        QtPassage passage = seedPassage(LocalDate.now().minusDays(1));

        mockMvc.perform(get("/api/v1/qt/passages/{id}/content-context", passage.getId()).with(systemBatch()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()))
                .andExpect(jsonPath("$.data.published").value(true));
    }

    @Test
    @DisplayName("SYSTEM_BATCH는 날짜로 콘텐츠 컨텍스트를 200으로 받는다")
    void 날짜_조회_시스템배치_200() throws Exception {
        LocalDate date = LocalDate.now().minusDays(1);
        QtPassage passage = seedPassage(date);

        mockMvc.perform(get("/api/v1/qt/content-context").param("qtDate", date.toString()).with(systemBatch()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()));
    }

    @Test
    @DisplayName("없는 id 조회는 404 (SYSTEM_BATCH라도)")
    void id_없으면_404() throws Exception {
        mockMvc.perform(get("/api/v1/qt/passages/{id}/content-context", 999999L).with(systemBatch()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("해당 날짜 본문이 없으면 404 (어댑터가 Optional.empty()로 변환)")
    void 날짜_없으면_404() throws Exception {
        mockMvc.perform(get("/api/v1/qt/content-context").param("qtDate", "1999-01-01").with(systemBatch()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("일반 사용자(ROLE_USER)는 미공개 본문 노출 차단으로 403")
    void 사용자_403() throws Exception {
        mockMvc.perform(get("/api/v1/qt/passages/{id}/content-context", 1L).with(user(123L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/qt/content-context").param("qtDate", "2026-06-10").with(user(123L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("미인증 요청은 401")
    void 미인증_401() throws Exception {
        mockMvc.perform(get("/api/v1/qt/passages/{id}/content-context", 1L))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/qt/content-context").param("qtDate", "2026-06-10"))
                .andExpect(status().isUnauthorized());
    }
}
