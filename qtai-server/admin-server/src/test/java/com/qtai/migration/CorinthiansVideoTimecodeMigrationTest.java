package com.qtai.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class CorinthiansVideoTimecodeMigrationTest {

    @Test
    void v37SeedIsIdempotentAndContainsNoPlayableUrlOrBibleText() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v37_corinthians_video;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(schemaSql().getBytes()));
            seedCorinthiansVerses(connection);

            ClassPathResource migration = new ClassPathResource(
                    "db/migration/V37__seed_corinthians_video_timecodes.sql");
            ScriptUtils.executeSqlScript(connection, migration);
            ScriptUtils.executeSqlScript(connection, migration);

            assertEquals(1, queryInt(connection, "SELECT COUNT(*) FROM source_videos"));
            assertEquals(0, queryInt(connection,
                    "SELECT COUNT(*) FROM source_videos WHERE video_url LIKE 'http%'"));
            assertEquals(0, queryInt(connection,
                    "SELECT COUNT(*) FROM source_videos WHERE video_url LIKE '%github%'"));
            assertEquals("UNCONFIGURED", queryString(connection,
                    "SELECT storage_provider FROM source_videos WHERE bible_book_id = 46"));
            assertEquals("INACTIVE", queryString(connection,
                    "SELECT status FROM source_videos WHERE bible_book_id = 46"));

            assertEquals(3, queryInt(connection, "SELECT COUNT(*) FROM bible_verse_video_segments"));
            assertEquals("0.000", queryString(connection,
                    "SELECT CAST(MIN(start_time_sec) AS VARCHAR) FROM bible_verse_video_segments"));
            assertEquals("30.000", queryString(connection,
                    "SELECT CAST(MAX(end_time_sec) AS VARCHAR) FROM bible_verse_video_segments"));
        }
    }

    private static void seedCorinthiansVerses(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO bible_books (id) VALUES (46)");
            statement.executeUpdate("""
                    INSERT INTO bible_verses (id, book_id, chapter_no, verse_no)
                    VALUES
                        (1001, 46, 1, 1),
                        (1002, 46, 1, 2),
                        (1003, 46, 2, 1)
                    """);
        }
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private static String schemaSql() {
        return """
                CREATE TABLE bible_books (
                    id SMALLINT PRIMARY KEY
                );

                CREATE TABLE bible_verses (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    book_id SMALLINT NOT NULL,
                    chapter_no SMALLINT NOT NULL,
                    verse_no SMALLINT NOT NULL
                );

                CREATE TABLE source_videos (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    bible_book_id SMALLINT NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    storage_provider VARCHAR(30) NOT NULL,
                    video_url VARCHAR(2048) NOT NULL,
                    duration_sec DECIMAL(10,3),
                    status VARCHAR(20) NOT NULL,
                    active_unique_key VARCHAR(20),
                    deleted_at DATETIME(6),
                    CONSTRAINT uk_source_videos_book_active UNIQUE (bible_book_id, active_unique_key)
                );

                CREATE TABLE bible_verse_video_segments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    bible_verse_id BIGINT NOT NULL,
                    source_video_id BIGINT NOT NULL,
                    start_time_sec DECIMAL(10,3) NOT NULL,
                    end_time_sec DECIMAL(10,3) NOT NULL,
                    deleted_at DATETIME(6),
                    CONSTRAINT uk_bv_video_segments_verse_source UNIQUE (bible_verse_id, source_video_id)
                );
                """;
    }
}
