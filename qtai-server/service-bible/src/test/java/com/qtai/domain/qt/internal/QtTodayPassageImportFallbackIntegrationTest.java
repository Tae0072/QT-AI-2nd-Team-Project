package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.qtai.bible.BibleServiceApplication;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 절 매핑 실패 시 본문 유지 폴백이 실제 트랜잭션에서 동작하는지 보장하는 통합 테스트.
 *
 * <p>회귀 가드: 절 매핑을 본문 저장과 같은 트랜잭션에서 처리하면, 빈 장에 대한 bible
 * {@code getVerses}(readOnly) 예외가 트랜잭션을 rollback-only로 만들어 커밋 시
 * {@code UnexpectedRollbackException}이 발생하고 본문까지 롤백된다. 매핑을 REQUIRES_NEW로
 * 분리했으므로 본문은 저장되고 매핑만 비어야 한다.
 */
@SpringBootTest(classes = BibleServiceApplication.class, properties = {
        "qt.today-source.sum.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QtTodayPassageImportFallbackIntegrationTest {

    private static final short FIRST_CORINTHIANS_BOOK_ID = 46;

    @Autowired
    private QtTodayPassageImportService importService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanAndSeed() {
        jdbcTemplate.update("DELETE FROM qt_passage_verses");
        jdbcTemplate.update("DELETE FROM qt_passages");
        jdbcTemplate.update("DELETE FROM bible_verses");
        jdbcTemplate.update("DELETE FROM bible_books");
        clearBibleBookCache();

        jdbcTemplate.update("""
                MERGE INTO bible_books (id, testament, code, korean_name, english_name, display_order)
                KEY(id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                FIRST_CORINTHIANS_BOOK_ID, "NEW", "1CO", "고린도전서", "1 Corinthians", FIRST_CORINTHIANS_BOOK_ID);
        // 9장 절만 시드하고 10장은 비워 둔다 → getVerses("1CO", 10, ...)가 BIBLE_VERSE_NOT_FOUND를 던진다.
        for (int verseNo = 1; verseNo <= 5; verseNo++) {
            jdbcTemplate.update("""
                    INSERT INTO bible_verses (book_id, chapter_no, verse_no, korean_text, english_text)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    FIRST_CORINTHIANS_BOOK_ID, 9, verseNo, "본문 " + verseNo, "Text " + verseNo);
        }
        clearBibleBookCache();
    }

    @Test
    @DisplayName("장 교차 본문에서 일부 장 절이 없어 매핑이 실패해도 본문은 저장되고 롤백되지 않는다")
    void importToday_crossChapterMissingVerses_keepsPassageWithoutRollback() {
        LocalDate qtDate = LocalDate.of(2026, 6, 15);
        SuTodayPassage passage = new SuTodayPassage(
                "스스로 종이 된 자유인", "고린도전서", "1 Corinthians",
                (short) 9, (short) 10, (short) 1, (short) 5,
                "고린도전서(1 Corinthians) 9:1-10:5");

        QtPassage[] saved = new QtPassage[1];
        assertThatCode(() -> saved[0] = importService.importToday(qtDate, passage))
                .doesNotThrowAnyException();

        assertThat(saved[0]).isNotNull();
        assertThat(saved[0].getId()).isNotNull();

        Long passageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qt_passages WHERE qt_date = ?", Long.class, java.sql.Date.valueOf(qtDate));
        assertThat(passageCount).as("본문은 저장되어야 한다").isEqualTo(1L);

        Long verseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qt_passage_verses WHERE qt_passage_id = ?", Long.class, saved[0].getId());
        assertThat(verseCount).as("절 매핑은 비어 백필 재시도 대상으로 남는다").isEqualTo(0L);
    }

    private void clearBibleBookCache() {
        var cache = cacheManager.getCache("bibleBooks");
        if (cache != null) {
            cache.clear();
        }
    }
}
