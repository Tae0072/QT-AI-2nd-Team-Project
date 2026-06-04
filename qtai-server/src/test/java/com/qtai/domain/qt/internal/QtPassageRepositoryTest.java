package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class QtPassageRepositoryTest {

    @Autowired
    private QtPassageRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("QT 본문 ID로 Flutter 연동용 권/장/절 범위를 조회한다")
    void findRangeByQtPassageId_returnsRange() {
        jdbcTemplate.update("""
                INSERT INTO bible_books
                    (id, testament, code, korean_name, english_name, display_order)
                VALUES
                    (46, 'NEW', '1CO', '고린도전서', '1 Corinthians', 46)
                """);
        jdbcTemplate.update("""
                INSERT INTO qt_passages
                    (id, qt_date, book_id, chapter, start_verse, end_verse, title, main_verse_ref, created_at, updated_at)
                VALUES
                    (7, DATE '2026-06-03', 46, 1, 10, 17, '분열을 멈추라', '고린도전서 1:10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        Optional<QtPassageRangeView> result = repository.findRangeByQtPassageId(7L);

        assertThat(result).isPresent();
        QtPassageRangeView range = result.orElseThrow();
        assertThat(range.getTestament()).isEqualTo("NEW");
        assertThat(range.getBookCode()).isEqualTo("1CO");
        assertThat(range.getKoreanBookName()).isEqualTo("고린도전서");
        assertThat(range.getEnglishBookName()).isEqualTo("1 Corinthians");
        assertThat(range.getChapter()).isEqualTo((short) 1);
        assertThat(range.getVerseFrom()).isEqualTo((short) 10);
        assertThat(range.getVerseTo()).isEqualTo((short) 17);
    }
}
