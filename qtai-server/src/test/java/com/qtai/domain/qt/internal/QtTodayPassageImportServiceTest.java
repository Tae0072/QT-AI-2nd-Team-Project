package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
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
    private QtTodayPassageImportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("성서유니온 오늘 본문 범위를 날짜 기준으로 새로 저장한다")
    void importToday_createsQtPassageForDate() {
        insertBibleBook();
        SuTodayPassage passage = new SuTodayPassage(
                "같은 말, 같은 마음, 같은 뜻",
                "고린도전서",
                "1 Corinthians",
                (short) 1,
                (short) 10,
                (short) 17,
                "고린도전서(1 Corinthians) 1:10-17"
        );

        QtPassage result = service.importToday(LocalDate.of(2026, 6, 2), passage);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getQtDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(result.getBookId()).isEqualTo((short) 46);
        assertThat(result.getTitle()).isEqualTo("같은 말, 같은 마음, 같은 뜻");
        assertThat(result.getChapter()).isEqualTo((short) 1);
        assertThat(result.getStartVerse()).isEqualTo((short) 10);
        assertThat(result.getEndVerse()).isEqualTo((short) 17);
        assertThat(result.getMainVerseRef()).isEqualTo("고린도전서(1 Corinthians) 1:10-17");
    }

    @Test
    @DisplayName("같은 날짜 QT 본문이 이미 있으면 성서유니온 변경 범위로 갱신한다")
    void importToday_updatesExistingQtPassageForDate() {
        insertBibleBook();
        jdbcTemplate.update("""
                INSERT INTO qt_passages
                    (id, qt_date, book_id, chapter, start_verse, end_verse, title, main_verse_ref, created_at, updated_at)
                VALUES
                    (30, DATE '2026-06-02', 46, 1, 1, 9, '이전 제목', '이전 범위', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        SuTodayPassage passage = new SuTodayPassage(
                "변경된 제목",
                "고린도전서",
                "1 Corinthians",
                (short) 1,
                (short) 10,
                (short) 17,
                "고린도전서(1 Corinthians) 1:10-17"
        );

        QtPassage result = service.importToday(LocalDate.of(2026, 6, 2), passage);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getTitle()).isEqualTo("변경된 제목");
        assertThat(result.getChapter()).isEqualTo((short) 1);
        assertThat(result.getStartVerse()).isEqualTo((short) 10);
        assertThat(result.getEndVerse()).isEqualTo((short) 17);
        assertThat(repository.findAll()).hasSize(1);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DB에 등록되지 않은 성경 권");
        assertThat(repository.findAll()).isEmpty();
    }

    private void insertBibleBook() {
        jdbcTemplate.update("""
                INSERT INTO bible_books
                    (id, testament, code, korean_name, english_name, display_order)
                VALUES
                    (46, 'NEW', '1CO', '고린도전서', '1 Corinthians', 46)
                """);
    }
}
