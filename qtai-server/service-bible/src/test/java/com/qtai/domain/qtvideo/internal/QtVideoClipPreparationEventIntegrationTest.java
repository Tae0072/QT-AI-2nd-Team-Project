package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qtai.bible.BibleServiceApplication;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import com.qtai.domain.qt.internal.QtPassage;
import com.qtai.domain.qt.internal.QtTodayPassageImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.Statement;
import java.time.LocalDate;

@SpringBootTest(classes = BibleServiceApplication.class, properties = {
        "qt.today-source.sum.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QtVideoClipPreparationEventIntegrationTest {

    private static final short FIRST_CORINTHIANS_BOOK_ID = 46;

    @Autowired
    private QtTodayPassageImportService importService;

    @Autowired
    private QtVideoClipRepository qtVideoClipRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Commits import, handles AFTER_COMMIT event, and persists QT video clip")
    void importCommit_publishesEventAndPersistsClip() {
        seedBibleBook();
        clearBibleBookCache();
        long verse1 = seedBibleVerse((short) 3, (short) 1);
        long verse2 = seedBibleVerse((short) 3, (short) 2);
        long verse3 = seedBibleVerse((short) 3, (short) 3);
        long sourceVideoId = seedSourceVideo();
        seedSegment(verse1, sourceVideoId, "10.000", "20.000");
        seedSegment(verse2, sourceVideoId, "20.000", "30.000");
        seedSegment(verse3, sourceVideoId, "30.000", "40.000");

        QtPassage passage = importService.importToday(
                LocalDate.of(2026, 6, 9),
                new SuTodayPassage(
                        "1 Corinthians QT event test",
                        "고린도전서",
                        "1 Corinthians",
                        (short) 3,
                        (short) 1,
                        (short) 3,
                        "1 Corinthians 3:1-3"
                )
        );

        var clip = qtVideoClipRepository.findByQtPassageIdAndActiveUniqueKey(
                passage.getId(),
                QtVideoClip.ACTIVE_UNIQUE_KEY
        );

        assertTrue(clip.isPresent());
        assertEquals(QtVideoClipStatus.APPROVED, clip.get().getStatus());
        assertEquals(QtVideoCompositionType.SINGLE_CUT, clip.get().getCompositionType());
        assertEquals("https://cdn.example.com/corinthians_full.mp4", clip.get().getVideoUrl());
        assertEquals(0, clip.get().getStartTimeSec().compareTo(new java.math.BigDecimal("10.000")));
        assertEquals(0, clip.get().getEndTimeSec().compareTo(new java.math.BigDecimal("40.000")));
    }

    private void seedBibleBook() {
        jdbcTemplate.update("""
                MERGE INTO bible_books (
                    id, testament, code, korean_name, english_name, display_order
                )
                KEY(id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                FIRST_CORINTHIANS_BOOK_ID,
                "NEW",
                "1CO",
                "고린도전서",
                "1 Corinthians",
                FIRST_CORINTHIANS_BOOK_ID);
    }

    private void clearBibleBookCache() {
        var cache = cacheManager.getCache("bibleBooks");
        if (cache != null) {
            cache.clear();
        }
    }

    private long seedBibleVerse(short chapter, short verse) {
        return insertAndReturnId("""
                INSERT INTO bible_verses (
                    book_id, chapter_no, verse_no, korean_text, english_text
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                FIRST_CORINTHIANS_BOOK_ID,
                chapter,
                verse,
                "test korean body " + verse,
                "test english body " + verse);
    }

    private long seedSourceVideo() {
        return insertAndReturnId("""
                INSERT INTO source_videos (
                    bible_book_id, title, storage_provider, video_url,
                    duration_sec, status, active_unique_key, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                FIRST_CORINTHIANS_BOOK_ID,
                "1 Corinthians full video",
                "EXTERNAL_URL",
                "https://cdn.example.com/corinthians_full.mp4",
                600.000,
                "ACTIVE",
                SourceVideo.ACTIVE_UNIQUE_KEY);
    }

    private void seedSegment(long bibleVerseId, long sourceVideoId, String startTimeSec, String endTimeSec) {
        jdbcTemplate.update("""
                INSERT INTO bible_verse_video_segments (
                    bible_verse_id, source_video_id, start_time_sec, end_time_sec,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                bibleVerseId,
                sourceVideoId,
                startTimeSec,
                endTimeSec);
    }

    private long insertAndReturnId(String sql, Object... params) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated key was not returned.");
        }
        return key.longValue();
    }
}
