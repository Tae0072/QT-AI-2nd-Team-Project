package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QtVideoControllerTest {

    private static final short FIRST_CORINTHIANS_BOOK_ID = 46;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @DisplayName("QT 영상 조회: 승인 클립이 있으면 200 envelope와 READY payload를 반환한다")
    void approvedClipReturnsReady() throws Exception {
        long passageId = seedPassage(LocalDate.now().minusDays(1), 1);
        SeededClip clip = seedClip(passageId, "APPROVED");

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passageId).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.clipId").value(clip.clipId()))
                .andExpect(jsonPath("$.data.qtPassageId").value(passageId))
                .andExpect(jsonPath("$.data.videoUrl").value("https://cdn.example.com/qt-2026-06-17.mp4"))
                .andExpect(jsonPath("$.data.sourceVideoId").value(clip.sourceVideoId()))
                .andExpect(jsonPath("$.data.compositionType").value("SINGLE_CUT"))
                .andExpect(jsonPath("$.data.clipStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("QT 영상 조회: 본문은 있지만 클립이 없으면 200 envelope와 MISSING payload를 반환한다")
    void missingClipReturnsMissing() throws Exception {
        long passageId = seedPassage(LocalDate.now().minusDays(2), 2);

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passageId).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("MISSING"))
                .andExpect(jsonPath("$.data.qtPassageId").value(passageId))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
    }

    @Test
    @DisplayName("QT 영상 조회: 실패 클립이 있으면 200 envelope와 FAILED payload를 반환한다")
    void failedClipReturnsFailed() throws Exception {
        long passageId = seedPassage(LocalDate.now().minusDays(3), 3);
        seedClip(passageId, "FAILED");

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passageId).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.qtPassageId").value(passageId))
                .andExpect(jsonPath("$.data.clipStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
    }

    @Test
    @DisplayName("QT 영상 조회: 숨김 클립이 있으면 200 envelope와 DISABLED payload를 반환한다")
    void hiddenClipReturnsDisabled() throws Exception {
        long passageId = seedPassage(LocalDate.now().minusDays(4), 4);
        seedClip(passageId, "HIDDEN");

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passageId).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.qtPassageId").value(passageId))
                .andExpect(jsonPath("$.data.clipStatus").value("HIDDEN"))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
    }

    @Test
    @DisplayName("QT 영상 조회: 잘못된 본문 ID는 400을 반환한다")
    void invalidIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", 0L).with(user(123L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("QT 영상 조회: 없는 본문 ID는 404를 반환한다")
    void notFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", 999999L).with(user(123L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("QT 영상 조회: 미인증 요청은 401을 반환한다")
    void unauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private long seedPassage(LocalDate qtDate, int dayOffset) {
        seedBibleBook();
        return insertAndReturnId("""
                INSERT INTO qt_passages (
                    qt_date, book_id, chapter, start_verse, end_verse,
                    title, main_verse_ref, status, published_at, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                qtDate,
                FIRST_CORINTHIANS_BOOK_ID,
                3,
                1,
                15,
                "1 Corinthians QT video test " + dayOffset,
                "1Co 3:1-15");
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

    private SeededClip seedClip(long qtPassageId, String status) {
        String activeUniqueKey = "APPROVED".equals(status) ? "ACTIVE" : null;
        long sourceVideoId = insertAndReturnId("""
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
                activeUniqueKey);

        long clipId = insertAndReturnId("""
                INSERT INTO qt_video_clips (
                    qt_passage_id, title, source_video_id, composition_type,
                    video_url, start_time_sec, end_time_sec, status,
                    active_unique_key, approved_at, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                qtPassageId,
                "1 Corinthians 3:1-15 QT video",
                sourceVideoId,
                "SINGLE_CUT",
                "https://cdn.example.com/qt-2026-06-17.mp4",
                10.000,
                90.000,
                status,
                activeUniqueKey);
        return new SeededClip(clipId, sourceVideoId);
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

    private record SeededClip(long clipId, long sourceVideoId) {
    }
}
