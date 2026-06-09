package com.qtai.bible;

import java.util.Optional;

import javax.sql.DataSource;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleBookRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB-per-service persistence 구성 활성 검증 (Inc1b).
 *
 * <p>{@code qtai.bible.persistence.enabled=true}로 전용 DataSource/EMF/JPA 리포지토리가 구성되는지,
 * 스키마가 만들어지고 리포지토리 라운드트립이 동작하는지 H2(MODE=MySQL)로 확인한다.
 * 운영은 MySQL + Flyway지만, 테스트는 MySQL 전용 DDL 구문 의존을 피하려 Flyway를 끄고
 * Hibernate create-drop으로 엔티티에서 스키마를 생성한다(ai-service persistence 테스트 패턴).
 * BibleBook은 protected 생성자(빌더 없음)라 데이터는 JDBC로 삽입 후 리포지토리로 조회한다.
 */
@SpringBootTest(
        classes = BibleServiceApplication.class,
        properties = {
                "qtai.bible.persistence.enabled=true",
                "qtai.bible.persistence.url=jdbc:h2:mem:bible_service_persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.bible.persistence.username=sa",
                "qtai.bible.persistence.driver-class-name=org.h2.Driver",
                "qtai.bible.persistence.ddl-auto=create-drop",
                "qtai.bible.persistence.flyway-enabled=false"
        }
)
class BibleServicePersistenceTest {

    @Autowired
    private BibleBookRepository bibleBookRepository;

    @Autowired
    @Qualifier("bibleServiceDataSource")
    private DataSource bibleServiceDataSource;

    @Test
    void persistenceEnabled_schemaAndRepositoryRoundTrip() {
        JdbcTemplate jdbc = new JdbcTemplate(bibleServiceDataSource);
        jdbc.update("INSERT INTO bible_books (id, testament, code, korean_name, english_name, display_order) "
                + "VALUES (1, 'OLD', 'GEN', '창세기', 'Genesis', 1)");

        Optional<BibleBook> found = bibleBookRepository.findByCode("GEN");

        assertThat(found).isPresent();
        assertThat(found.get().getKoreanName()).isEqualTo("창세기");
        assertThat(found.get().getTestament()).isEqualTo(BibleBook.Testament.OLD);
        assertThat(bibleBookRepository.findAllByOrderByDisplayOrderAsc()).hasSize(1);
    }
}
