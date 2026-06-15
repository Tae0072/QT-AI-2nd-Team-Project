package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class AiPromptVersionMigrationTest {

    private static String lastSha2Value;

    @Test
    void v48RecalculatesExplanationContentHashWithSameBytesAsJava() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:v48-prompt-migration;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbcTemplate.execute("""
                    CREATE ALIAS IF NOT EXISTS SHA2
                    FOR "com.qtai.domain.ai.internal.AiPromptVersionMigrationTest.sha2"
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE ai_prompt_versions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        prompt_type VARCHAR(30) NOT NULL,
                        version VARCHAR(30) NOT NULL,
                        content_hash VARCHAR(100) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        system_prompt LONGTEXT,
                        user_prompt_template LONGTEXT,
                        model_name VARCHAR(100),
                        temperature DOUBLE,
                        max_tokens INT
                    )
                    """);
            jdbcTemplate.update("""
                    INSERT INTO ai_prompt_versions (
                        prompt_type, version, content_hash, status, system_prompt,
                        user_prompt_template, model_name, temperature, max_tokens
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "EXPLANATION",
                    "2026.06.custom",
                    "legacy-hash",
                    "ACTIVE",
                    "custom system prompt",
                    "custom user instruction",
                    "deepseek-chat",
                    null,
                    null
            );

            lastSha2Value = null;
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V48__fix_explanation_system_prompt.sql")
            );

            String expectedContent = String.join("\n",
                    "EXPLANATION",
                    "2026.06.custom",
                    AiPromptVersion.defaultSystemPrompt(),
                    "custom user instruction",
                    "deepseek-chat",
                    "0.2",
                    "2000"
            );
            String expectedHash = AiPromptManagementService.contentHash(
                    "EXPLANATION",
                    "2026.06.custom",
                    "custom user instruction",
                    "deepseek-chat",
                    0.2,
                    2000
            );
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT system_prompt FROM ai_prompt_versions WHERE version = ?",
                    String.class,
                    "2026.06.custom"
            )).isEqualTo(AiPromptVersion.defaultSystemPrompt());
            assertThat(lastSha2Value).isEqualTo(expectedContent);
            String actualHash = jdbcTemplate.queryForObject(
                    "SELECT content_hash FROM ai_prompt_versions WHERE version = ?",
                    String.class,
                    "2026.06.custom"
            );

            assertThat(actualHash).isEqualTo(expectedHash);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT temperature FROM ai_prompt_versions WHERE version = ?",
                    Double.class,
                    "2026.06.custom"
            )).isEqualTo(0.2);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT max_tokens FROM ai_prompt_versions WHERE version = ?",
                    Integer.class,
                    "2026.06.custom"
            )).isEqualTo(2000);
        }
    }

    public static String sha2(String value, int bits) throws NoSuchAlgorithmException {
        if (bits != 256) {
            throw new IllegalArgumentException("Only SHA-256 is supported in this test alias");
        }
        lastSha2Value = value;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
