package com.qtai.bible;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qtai.domain.bible.internal.BibleBookRepository;
import com.qtai.domain.qt.internal.QtPassage;
import com.qtai.domain.qt.internal.QtPassageRepository;
import com.qtai.domain.qtvideo.internal.QtVideoClip;
import com.qtai.domain.qtvideo.internal.QtVideoClipStatus;
import com.qtai.domain.qtvideo.internal.SourceVideo;
import com.qtai.domain.qtvideo.internal.SourceVideoStorageProvider;
import com.qtai.support.TestEntityFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    private BibleBookRepository bibleBookRepository;

    @Autowired
    private QtPassageRepository qtPassageRepository;

    @Autowired
    private EntityManager entityManager;

    private static RequestPostProcessor user(long memberId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @DisplayName("QT 영상 조회: 승인 클립이 있으면 200 envelope와 READY payload를 반환한다")
    void 승인_클립_조회_200() throws Exception {
        QtPassage passage = seedPassage(LocalDate.now().minusDays(1), 1);
        QtVideoClip clip = seedApprovedClip(passage.getId());

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passage.getId()).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.clipId").value(clip.getId()))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()))
                .andExpect(jsonPath("$.data.videoUrl").value("https://cdn.example.com/qt-2026-06-17.mp4"))
                .andExpect(jsonPath("$.data.sourceVideoId").value(clip.getSourceVideo().getId()))
                .andExpect(jsonPath("$.data.compositionType").value("SINGLE_CUT"))
                .andExpect(jsonPath("$.data.clipStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("QT 영상 조회: 본문은 있지만 클립이 없으면 200 envelope와 MISSING payload를 반환한다")
    void 클립_없음_200_missing() throws Exception {
        QtPassage passage = seedPassage(LocalDate.now().minusDays(2), 2);

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passage.getId()).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("MISSING"))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
    }

    @Test
    @DisplayName("QT 영상 조회: 실패 클립이 있으면 200 envelope와 FAILED payload를 반환한다")
    void 실패_클립_200_failed() throws Exception {
        QtPassage passage = seedPassage(LocalDate.now().minusDays(3), 3);
        seedClip(passage.getId(), QtVideoClipStatus.FAILED);

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passage.getId()).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()))
                .andExpect(jsonPath("$.data.clipStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
    }

    @Test
    @DisplayName("QT 영상 조회: 숨김 클립이 있으면 200 envelope와 DISABLED payload를 반환한다")
    void 숨김_클립_200_disabled() throws Exception {
        QtPassage passage = seedPassage(LocalDate.now().minusDays(4), 4);
        seedClip(passage.getId(), QtVideoClipStatus.HIDDEN);

        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", passage.getId()).with(user(123L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.qtPassageId").value(passage.getId()))
                .andExpect(jsonPath("$.data.clipStatus").value("HIDDEN"))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
    }

    @Test
    @DisplayName("QT 영상 조회: 잘못된 본문 ID는 400을 반환한다")
    void 잘못된_id_400() throws Exception {
        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", 0L).with(user(123L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("QT 영상 조회: 없는 본문 ID는 404를 반환한다")
    void 없는_본문_404() throws Exception {
        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", 999999L).with(user(123L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("QT 영상 조회: 미인증 요청은 401을 반환한다")
    void 미인증_401() throws Exception {
        mockMvc.perform(get("/api/v1/qt/{qtPassageId}/video", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private QtPassage seedPassage(LocalDate qtDate, int dayOffset) {
        seedBibleBook();
        return qtPassageRepository.save(QtPassage.create(
                qtDate,
                FIRST_CORINTHIANS_BOOK_ID,
                (short) 3,
                (short) 1,
                (short) 15,
                "고린도전서 QT 영상 테스트 " + dayOffset,
                "고전 3:1-15"
        ));
    }

    private void seedBibleBook() {
        if (bibleBookRepository.existsById(FIRST_CORINTHIANS_BOOK_ID)) {
            return;
        }
        bibleBookRepository.save(TestEntityFactory.bibleBook(
                FIRST_CORINTHIANS_BOOK_ID,
                "1CO",
                "고린도전서",
                "1 Corinthians",
                FIRST_CORINTHIANS_BOOK_ID
        ));
    }

    private QtVideoClip seedApprovedClip(Long qtPassageId) {
        return seedClip(qtPassageId, QtVideoClipStatus.APPROVED);
    }

    private QtVideoClip seedClip(Long qtPassageId, QtVideoClipStatus status) {
        SourceVideo sourceVideo = SourceVideo.active(
                FIRST_CORINTHIANS_BOOK_ID,
                "1 Corinthians full video",
                SourceVideoStorageProvider.EXTERNAL_URL,
                "https://cdn.example.com/corinthians_full.mp4",
                new BigDecimal("600.000")
        );
        entityManager.persist(sourceVideo);

        QtVideoClip clip = TestEntityFactory.qtVideoClip(
                null,
                qtPassageId,
                sourceVideo,
                "https://cdn.example.com/qt-2026-06-17.mp4",
                status
        );
        entityManager.persist(clip);
        entityManager.flush();
        return clip;
    }
}
