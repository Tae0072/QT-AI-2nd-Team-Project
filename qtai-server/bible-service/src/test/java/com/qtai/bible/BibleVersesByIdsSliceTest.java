package com.qtai.bible;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inc3c-1 — verse ID 배치 조회 엔드포인트(`GET /api/v1/bible/verses/by-ids`) 슬라이스 검증.
 *
 * <p>note 등 소비자가 HTTP 어댑터로 전환할 때 사용할 엔드포인트. inbound+persistence 활성 컨텍스트에서
 * 게이트웨이 신원 헤더와 함께 ids 쿼리로 절들을 일괄 조회한다.
 */
@SpringBootTest(classes = BibleServiceApplication.class)
@AutoConfigureMockMvc
class BibleVersesByIdsSliceTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String dbName = "bible_service_byids_" + UUID.randomUUID().toString().replace("-", "");
        registry.add("qtai.bible.inbound.enabled", () -> "true");
        registry.add("qtai.bible.persistence.enabled", () -> "true");
        registry.add("qtai.bible.persistence.url",
                () -> "jdbc:h2:mem:" + dbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("qtai.bible.persistence.username", () -> "sa");
        registry.add("qtai.bible.persistence.driver-class-name", () -> "org.h2.Driver");
        registry.add("qtai.bible.persistence.ddl-auto", () -> "create-drop");
        registry.add("qtai.bible.persistence.flyway-enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("bibleServiceDataSource")
    private DataSource bibleServiceDataSource;

    @BeforeEach
    void seed() {
        JdbcTemplate jdbc = new JdbcTemplate(bibleServiceDataSource);
        jdbc.update("MERGE INTO bible_books (id, testament, code, korean_name, english_name, display_order) "
                + "VALUES (1, 'OLD', 'GEN', '창세기', 'Genesis', 1)");
        jdbc.update("MERGE INTO bible_verses (id, book_id, chapter_no, verse_no, korean_text, english_text) "
                + "VALUES (10, 1, 1, 1, '태초에 하나님이', 'In the beginning God')");
        jdbc.update("MERGE INTO bible_verses (id, book_id, chapter_no, verse_no, korean_text, english_text) "
                + "VALUES (11, 1, 1, 2, '땅이 혼돈하고', 'And the earth was without form')");
    }

    @Test
    void getVersesByIds_withGatewayHeaders_returnsRequestedVerses() throws Exception {
        mockMvc.perform(get("/api/v1/bible/verses/by-ids")
                        .param("ids", "10", "11")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getVersesByIds_missingId_returns404_allOrNothing() throws Exception {
        // 정책: all-or-nothing — 요청 ID 중 하나라도(999) 없으면 404 B0002 (500 누출 아님)
        mockMvc.perform(get("/api/v1/bible/verses/by-ids")
                        .param("ids", "10", "999")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("B0002"));
    }

    @Test
    void getVersesByIds_emptyIds_returns400() throws Exception {
        // ids 파라미터 누락 → 400 + C0002
        mockMvc.perform(get("/api/v1/bible/verses/by-ids")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    void getVersesByIds_overLimit_returns400() throws Exception {
        String[] tooMany = new String[201];
        for (int i = 0; i < tooMany.length; i++) {
            tooMany[i] = String.valueOf(i + 1);
        }
        mockMvc.perform(get("/api/v1/bible/verses/by-ids")
                        .param("ids", tooMany)
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C0002"));
    }

    @Test
    void getVersesByIds_withoutGatewayHeaders_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bible/verses/by-ids").param("ids", "10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }
}
