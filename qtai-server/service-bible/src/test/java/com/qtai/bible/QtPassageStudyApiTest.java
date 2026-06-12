package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleBookRepository;
import com.qtai.domain.bible.internal.BibleRepository;
import com.qtai.domain.bible.internal.BibleVerse;
import com.qtai.domain.qt.internal.QtPassage;
import com.qtai.domain.qt.internal.QtPassageRepository;
import com.qtai.domain.qt.internal.QtPassageVerse;
import com.qtai.domain.qt.internal.QtPassageVerseRepository;
import com.qtai.domain.study.internal.VerseExplanationRepository;
import com.qtai.domain.study.internal.VerseExplanationStatus;
import com.qtai.support.TestEntityFactory;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 {@code GET /api/v1/qt/passage-study} MockMvc 통합 테스트.
 *
 * <p>성경 본문 전체 페이지의 해설 진입점 가용성 조회 — 라우팅·인증·정상/부정 분기(해설 있음/
 * 매핑 없음/권 미존재)와 응답 계약을 고정한다. 인증은 실제 JWT 없이 principal=Long을 주입한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QtPassageStudyApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BibleBookRepository bibleBookRepository;
    @Autowired private BibleRepository bibleRepository;
    @Autowired private QtPassageRepository qtPassageRepository;
    @Autowired private QtPassageVerseRepository qtPassageVerseRepository;
    @Autowired private VerseExplanationRepository verseExplanationRepository;
    @Autowired private CacheManager cacheManager;

    // @Cacheable("bibleBooks")는 컨텍스트 캐시라 트랜잭션 롤백과 무관 — 앞선 @SpringBootTest가
    // 캐싱한 권 목록이 남아 새로 시드한 권을 못 찾는 일을 막기 위해 각 테스트 전 무효화한다.
    @BeforeEach
    void clearBibleBooksCache() {
        var cache = cacheManager.getCache("bibleBooks");
        if (cache != null) {
            cache.clear();
        }
    }

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private BibleVerse seedGenesis1_1() {
        BibleBook book = bibleBookRepository.save(
                TestEntityFactory.bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1));
        return bibleRepository.save(TestEntityFactory.bibleVerse(book, (short) 1, (short) 1));
    }

    @Test
    @DisplayName("해설 있음 — 범위를 포함하는 QT 본문에 승인 해설이 있으면 qtPassageId·hasExplanation=true")
    void 해설_있음_200() throws Exception {
        BibleVerse verse = seedGenesis1_1();
        QtPassage passage = qtPassageRepository.save(
                QtPassage.create(LocalDate.now().minusDays(1),
                        (short) 1, (short) 1, (short) 1, (short) 1, "제목", "창 1:1"));
        qtPassageVerseRepository.save(QtPassageVerse.create(passage.getId(), verse.getId(), (short) 1));
        verseExplanationRepository.save(TestEntityFactory.verseExplanation(
                verse.getId(), VerseExplanationStatus.APPROVED, "ACTIVE", "요약"));

        mockMvc.perform(get("/api/v1/qt/passage-study")
                        .param("bookCode", "GEN").param("chapter", "1")
                        .param("verseFrom", "1").param("verseTo", "1")
                        .with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()))
                .andExpect(jsonPath("$.data.hasExplanation").value(true));
    }

    @Test
    @DisplayName("매핑 없음 — 본문은 있으나 QT 매핑/해설이 없으면 hasExplanation=false")
    void 매핑_없음_200() throws Exception {
        seedGenesis1_1();

        mockMvc.perform(get("/api/v1/qt/passage-study")
                        .param("bookCode", "GEN").param("chapter", "1")
                        .param("verseFrom", "1").param("verseTo", "1")
                        .with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasExplanation").value(false));
    }

    @Test
    @DisplayName("권 미존재 — 알 수 없는 bookCode는 hasExplanation=false")
    void 권_미존재_200() throws Exception {
        mockMvc.perform(get("/api/v1/qt/passage-study")
                        .param("bookCode", "ZZZ").param("chapter", "1")
                        .param("verseFrom", "1").param("verseTo", "1")
                        .with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasExplanation").value(false));
    }

    @Test
    @DisplayName("미인증 요청은 401")
    void 미인증_401() throws Exception {
        mockMvc.perform(get("/api/v1/qt/passage-study")
                        .param("bookCode", "GEN").param("chapter", "1")
                        .param("verseFrom", "1").param("verseTo", "1"))
                .andExpect(status().isUnauthorized());
    }
}
