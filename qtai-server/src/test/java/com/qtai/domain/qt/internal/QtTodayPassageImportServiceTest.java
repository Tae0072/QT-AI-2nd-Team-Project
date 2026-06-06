package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({JpaAuditingConfig.class, QtTodayPassageImportService.class})
@ActiveProfiles("test")
class QtTodayPassageImportServiceTest {

    @Autowired
    private QtPassageRepository repository;

    @Autowired
    private QtPassageVerseRepository verseMappingRepository;

    @Autowired
    private QtTodayPassageImportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ListBibleBooksUseCase listBibleBooksUseCase;

    @MockBean
    private GetBibleVerseUseCase getBibleVerseUseCase;

    @BeforeEach
    void setUp() {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(
                new BibleBookResponse(46, "NEW", "1CO", "고린도전서", "1 Corinthians", 46)
        ));
        when(getBibleVerseUseCase.getVerses(eq("1CO"), eq(1), eq(10), eq(17)))
                .thenReturn(range(1010L, 1011L, 1012L, 1013L, 1014L, 1015L, 1016L, 1017L));
    }

    @Test
    @DisplayName("성서유니온 오늘 본문 범위를 날짜 기준으로 새로 저장하고 절 매핑도 함께 저장한다")
    void importToday_createsQtPassageForDateWithVerseMappings() {
        SuTodayPassage passage = corinthiansPassage("같은 말, 같은 마음, 같은 뜻");

        QtPassage result = service.importToday(LocalDate.of(2026, 6, 2), passage);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getQtDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(result.getBookId()).isEqualTo((short) 46);
        assertThat(result.getTitle()).isEqualTo("같은 말, 같은 마음, 같은 뜻");
        assertThat(result.getChapter()).isEqualTo((short) 1);
        assertThat(result.getStartVerse()).isEqualTo((short) 10);
        assertThat(result.getEndVerse()).isEqualTo((short) 17);
        assertThat(result.getMainVerseRef()).isEqualTo("고린도전서(1 Corinthians) 1:10-17");

        // 핵심 회귀 검증 — AI 시딩·학습 콘텐츠가 의존하는 qt_passage_verses가 채워져야 한다
        List<QtPassageVerse> mappings =
                verseMappingRepository.findByQtPassageIdOrderByDisplayOrderAsc(result.getId());
        assertThat(mappings).hasSize(8);
        assertThat(mappings.get(0).getBibleVerseId()).isEqualTo(1010L);
        assertThat(mappings.get(0).getDisplayOrder()).isEqualTo((short) 1);
        assertThat(mappings.get(7).getBibleVerseId()).isEqualTo(1017L);
        assertThat(mappings.get(7).getDisplayOrder()).isEqualTo((short) 8);
    }

    @Test
    @DisplayName("같은 날짜 본문 갱신 시 절 매핑도 새 범위로 교체된다")
    void importToday_updatesExistingQtPassageAndReplacesMappings() {
        jdbcTemplate.update("""
                INSERT INTO qt_passages
                    (id, qt_date, book_id, chapter, start_verse, end_verse, title, main_verse_ref, created_at, updated_at)
                VALUES
                    (30, DATE '2026-06-02', 46, 1, 1, 9, '이전 제목', '이전 범위', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO qt_passage_verses (id, qt_passage_id, bible_verse_id, display_order)
                VALUES (900, 30, 999L, 1)
                """);
        SuTodayPassage passage = corinthiansPassage("변경된 제목");

        QtPassage result = service.importToday(LocalDate.of(2026, 6, 2), passage);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getTitle()).isEqualTo("변경된 제목");
        assertThat(repository.findAll()).hasSize(1);

        List<QtPassageVerse> mappings =
                verseMappingRepository.findByQtPassageIdOrderByDisplayOrderAsc(30L);
        assertThat(mappings).hasSize(8);
        assertThat(mappings).extracting(QtPassageVerse::getBibleVerseId).doesNotContain(999L);
    }

    @Test
    @DisplayName("절 매핑 조회가 실패해도 본문 반영은 유지된다 (백필 재시도 대상)")
    void importToday_keepsPassageWhenVerseMappingFails() {
        when(getBibleVerseUseCase.getVerses(anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND));
        SuTodayPassage passage = corinthiansPassage("매핑 실패 케이스");

        QtPassage result = service.importToday(LocalDate.of(2026, 6, 2), passage);

        assertThat(result.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(1);
        assertThat(verseMappingRepository.findByQtPassageIdOrderByDisplayOrderAsc(result.getId())).isEmpty();
    }

    @Test
    @DisplayName("DB에 없는 영문 권명이면 QT 본문을 저장하지 않는다")
    void importToday_rejectsUnknownEnglishBookName() {
        SuTodayPassage passage = new SuTodayPassage(
                "제목",
                "알 수 없는 권",
                "Unknown Book",
                (short) 1,
                (short) 1,
                (short) 2,
                "알 수 없는 권(Unknown Book) 1:1-2"
        );

        assertThatThrownBy(() -> service.importToday(LocalDate.of(2026, 6, 2), passage))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DB에 등록되지 않은 성경 권")
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.BIBLE_BOOK_NOT_FOUND));
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("백필 — 절 매핑이 비어 있는 과거 본문을 채운다")
    void backfill_fillsPassagesWithoutVerseMappings() {
        jdbcTemplate.update("""
                INSERT INTO qt_passages
                    (id, qt_date, book_id, chapter, start_verse, end_verse, title, main_verse_ref, created_at, updated_at)
                VALUES
                    (40, DATE '2026-05-30', 46, 1, 10, 17, '백필 대상', '고린도전서 1:10-17', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        int filled = service.backfillMissingVerseMappings();

        assertThat(filled).isEqualTo(1);
        assertThat(verseMappingRepository.findByQtPassageIdOrderByDisplayOrderAsc(40L)).hasSize(8);
        // 두 번째 실행은 채울 것이 없어야 한다(멱등)
        assertThat(service.backfillMissingVerseMappings()).isZero();
    }

    private static SuTodayPassage corinthiansPassage(String title) {
        return new SuTodayPassage(
                title,
                "고린도전서",
                "1 Corinthians",
                (short) 1,
                (short) 10,
                (short) 17,
                "고린도전서(1 Corinthians) 1:10-17"
        );
    }

    private static BibleVerseRangeResponse range(Long... verseIds) {
        List<BibleVerseResponse> verses = java.util.Arrays.stream(verseIds)
                .map(id -> new BibleVerseResponse(id, "1CO", 1, 10 + (int) (id - 1010L), "본문", null))
                .toList();
        return new BibleVerseRangeResponse(null, verses);
    }
}
