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
 * Inc2b 회귀 — 게이트웨이 우회 2차 방어선(공유 토큰)이 통합 경로(MockMvc)에서 강제되는지 검증.
 *
 * <p>{@code qtai.bible.gateway.shared-token}이 설정된 상태에서, 신원 헤더가 있어도 게이트웨이가 주입하는
 * {@code X-Gateway-Token}이 일치해야 200, 불일치/누락이면 401임을 실증한다(필터 단위 테스트와 별개로
 * 컨트롤러까지 도달하는 통합 경로 회귀 안전망). 게이트웨이는 Inc2b에서 이 토큰을 {@code AddRequestHeader}로 주입한다.
 */
@SpringBootTest(classes = BibleServiceApplication.class)
@AutoConfigureMockMvc
class BibleGatewayTokenSliceTest {

    private static final String SHARED_TOKEN = "test-gateway-shared-token";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String dbName = "bible_service_token_" + UUID.randomUUID().toString().replace("-", "");
        registry.add("qtai.bible.inbound.enabled", () -> "true");
        registry.add("qtai.bible.gateway.shared-token", () -> SHARED_TOKEN);
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
        new JdbcTemplate(bibleServiceDataSource).update(
                "MERGE INTO bible_books (id, testament, code, korean_name, english_name, display_order) "
                        + "VALUES (1, 'OLD', 'GEN', '창세기', 'Genesis', 1)");
    }

    @Test
    void identityHeaders_withMatchingGatewayToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/bible/books")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER")
                        .header("X-Gateway-Token", SHARED_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void systemCall_validTokenWithoutIdentityHeaders_returns200() throws Exception {
        // 내부 SYSTEM 서비스 호출(배치/캐시 경계) — 사용자 헤더 없이 공유 토큰만으로 통과
        mockMvc.perform(get("/api/v1/bible/books")
                        .header("X-Gateway-Token", SHARED_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void identityHeaders_withWrongGatewayToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bible/books")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER")
                        .header("X-Gateway-Token", "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }

    @Test
    void identityHeaders_withoutGatewayToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bible/books")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }
}
