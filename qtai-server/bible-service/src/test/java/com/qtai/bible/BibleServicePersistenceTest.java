package com.qtai.bible;

import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleBookRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB-per-service persistence 구성 활성 검증 (Inc1b).
 *
 * <p>{@code qtai.bible.persistence.enabled=true}로 전용 DataSource/EMF/JPA 리포지토리가 구성되는지,
 * 스키마가 만들어지고 리포지토리 라운드트립이 동작하는지 H2(MODE=MySQL)로 확인한다.
 * 운영은 MySQL + Flyway지만, 테스트는 MySQL 전용 DDL 구문 의존을 피하려 Flyway를 끄고
 * Hibernate create-drop으로 엔티티에서 스키마를 생성한다(ai-service persistence 테스트 패턴).
 * BibleBook은 protected 생성자(빌더 없음)라 데이터는 JDBC로 삽입 후 리포지토리로 조회한다.
 *
 * <p><b>참고</b>: 이 테스트는 {@code com.qtai.domain.bible.internal}(Entity·Repository)에 직접 의존한다.
 * 일반적으로 도메인의 internal 직접 접근은 금지(ArchUnit)지만, bible-service는 bible 도메인의 <b>소유 모듈</b>이라
 * 자기 internal 접근은 경계 위반이 아니다. 이 직접 의존은 추출 검증(Inc1b) 한정이며, Inc3에서 소비자 경로가
 * HTTP 어댑터로 전환되면 도메인 간 접근은 {@code api/UseCase}로만 이뤄진다.
 */
@SpringBootTest(classes = BibleServiceApplication.class)
class BibleServicePersistenceTest {

    @DynamicPropertySource
    static void persistenceProperties(DynamicPropertyRegistry registry) {
        // DB명에 무작위 suffix → 테스트 클래스 간 인메모리 DB 간섭 차단
        String dbName = "bible_service_persistence_" + UUID.randomUUID().toString().replace("-", "");
        registry.add("qtai.bible.persistence.enabled", () -> "true");
        registry.add("qtai.bible.persistence.url",
                () -> "jdbc:h2:mem:" + dbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("qtai.bible.persistence.username", () -> "sa");
        registry.add("qtai.bible.persistence.driver-class-name", () -> "org.h2.Driver");
        registry.add("qtai.bible.persistence.ddl-auto", () -> "create-drop");
        registry.add("qtai.bible.persistence.flyway-enabled", () -> "false");
    }

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

        // 부정 경로 — 없는 코드는 빈 결과
        assertThat(bibleBookRepository.findByCode("XXX")).isEmpty();
    }
}
