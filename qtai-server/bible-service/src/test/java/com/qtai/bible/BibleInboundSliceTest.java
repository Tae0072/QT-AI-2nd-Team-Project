package com.qtai.bible;

import java.util.UUID;

import javax.sql.DataSource;

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
 * Inc2a — inbound 활성 시 bible-service가 인증 필터 뒤에서 정상 서빙하는지 슬라이스 검증.
 *
 * <p>{@code qtai.bible.inbound.enabled=true} + persistence 활성 컨텍스트에서 {@code BibleController}와
 * {@link GatewayHeaderAuthenticationFilter}가 함께 동작한다. 게이트웨이가 주입하는 신원 헤더가 있으면 200,
 * 없으면(=게이트웨이 미경유) 401임을 실증한다. 이 시점 게이트웨이는 여전히 모놀리식으로 라우팅하며(트래픽 없음),
 * 실제 라우트 전환은 Inc2b(게이트웨이)에서 수행한다.
 */
@SpringBootTest(classes = BibleServiceApplication.class)
@AutoConfigureMockMvc
class BibleInboundSliceTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String dbName = "bible_service_inbound_" + UUID.randomUUID().toString().replace("-", "");
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

    @Test
    void listBooks_withGatewayIdentityHeaders_returns200() throws Exception {
        new JdbcTemplate(bibleServiceDataSource).update(
                "INSERT INTO bible_books (id, testament, code, korean_name, english_name, display_order) "
                        + "VALUES (1, 'OLD', 'GEN', '창세기', 'Genesis', 1)");

        mockMvc.perform(get("/api/v1/bible/books")
                        .header("X-Member-Id", "42")
                        .header("X-Member-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void listBooks_withoutGatewayHeaders_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/bible/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("M0002"));
    }
}
