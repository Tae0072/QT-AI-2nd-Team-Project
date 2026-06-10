package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleBookRepository;
import com.qtai.domain.bible.internal.BibleRepository;
import com.qtai.domain.bible.internal.BibleVerse;
import com.qtai.support.TestEntityFactory;
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
 * service-bible 구절 조회 내부 엔드포인트(서비스 간 호출용) MockMvc 통합 테스트.
 *
 * <p>RestClient 통합(Day3)에서 service-note가 호출하는 {@code GET /api/v1/bible/verses/{id}}와
 * {@code GET /api/v1/bible/verses/by-ids} 두 엔드포인트의 라우팅·인증·정상/404 응답을 고정한다.
 * 인증은 실제 JWT 발급 없이 SecurityContext에 principal=Long 인증을 주입한다(실서버 필터와 동일 형태).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BibleVerseLookupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BibleBookRepository bibleBookRepository;

    @Autowired
    private BibleRepository bibleRepository;

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private long seedVerse() {
        BibleBook book = bibleBookRepository.save(
                TestEntityFactory.bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1));
        BibleVerse verse = bibleRepository.save(
                TestEntityFactory.bibleVerse(book, (short) 1, (short) 1));
        return verse.getId();
    }

    @Test
    @DisplayName("단건 조회 — 인증된 사용자는 구절 메타를 200으로 받는다")
    void 단건_조회_200() throws Exception {
        long verseId = seedVerse();

        mockMvc.perform(get("/api/v1/bible/verses/{verseId}", verseId).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(verseId))
                .andExpect(jsonPath("$.data.bookCode").value("GEN"))
                .andExpect(jsonPath("$.data.chapterNo").value(1))
                .andExpect(jsonPath("$.data.verseNo").value(1));
    }

    @Test
    @DisplayName("다건 조회(by-ids) — 요청 id 전부를 200 리스트로 받는다")
    void 다건_조회_200() throws Exception {
        long verseId = seedVerse();

        mockMvc.perform(get("/api/v1/bible/verses/by-ids")
                        .param("ids", String.valueOf(verseId))
                        .with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(verseId))
                .andExpect(jsonPath("$.data[0].bookCode").value("GEN"));
    }

    @Test
    @DisplayName("없는 구절 단건 조회는 404")
    void 단건_없으면_404() throws Exception {
        mockMvc.perform(get("/api/v1/bible/verses/{verseId}", 999999L).with(user(123L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("미인증 요청은 401 (라우팅 추가가 보호를 깨지 않음)")
    void 미인증_401() throws Exception {
        mockMvc.perform(get("/api/v1/bible/verses/{verseId}", 1L))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bible/verses/by-ids").param("ids", "1"))
                .andExpect(status().isUnauthorized());
    }
}
