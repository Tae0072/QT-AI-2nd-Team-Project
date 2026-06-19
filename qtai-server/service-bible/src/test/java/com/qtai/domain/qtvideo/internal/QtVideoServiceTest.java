package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qtvideo.api.dto.QtVideoClipResponse;
import com.qtai.support.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class QtVideoServiceTest {

    @Mock private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    @Mock private QtVideoClipRepository qtVideoClipRepository;

    private QtVideoService service;

    // [임시 2026-06-19] 폴백의 '오늘' 판정을 결정적으로 만드는 고정 시계(오늘 = 2026-06-19 KST). 원복 시 제거.
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 6, 19).atStartOfDay(KST).toInstant(), KST);

    @BeforeEach
    void setUp() {
        service = new QtVideoService(getQtPassageContentContextUseCase, qtVideoClipRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("승인된 QT 영상 클립이 있으면 READY를 반환한다")
    void approvedClipExists_ready() {
        when(getQtPassageContentContextUseCase.getContentContext(4L))
                .thenReturn(context(4L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip clip = TestEntityFactory.qtVideoClip(
                10L, 4L, sourceVideo, "https://cdn.example.com/qt-2026-06-17.mp4");
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                4L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of(clip));

        QtVideoClipResponse result = service.getVideo(4L);

        assertEquals("READY", result.status());
        assertEquals(10L, result.clipId());
        assertEquals(4L, result.qtPassageId());
        assertEquals("https://cdn.example.com/qt-2026-06-17.mp4", result.videoUrl());
        assertEquals(1L, result.sourceVideoId());
        assertEquals("SINGLE_CUT", result.compositionType());
        assertEquals("APPROVED", result.clipStatus());
    }

    @Test
    @DisplayName("사용자 노출 후보 클립이 없으면 MISSING을 반환한다")
    void approvedClipMissing_missing() {
        when(getQtPassageContentContextUseCase.getContentContext(5L))
                .thenReturn(context(5L, true));
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                5L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of());

        QtVideoClipResponse result = service.getVideo(5L);

        assertEquals("MISSING", result.status());
        assertEquals(5L, result.qtPassageId());
        assertNull(result.videoUrl());
    }

    @Test
    @DisplayName("실패 클립이 있으면 FAILED를 반환한다")
    void failedClipExists_failed() {
        when(getQtPassageContentContextUseCase.getContentContext(8L))
                .thenReturn(context(8L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip clip = TestEntityFactory.qtVideoClip(
                12L,
                8L,
                sourceVideo,
                "https://cdn.example.com/qt-failed.mp4",
                QtVideoClipStatus.FAILED);
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                8L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of(clip));

        QtVideoClipResponse result = service.getVideo(8L);

        assertEquals("FAILED", result.status());
        assertEquals(8L, result.qtPassageId());
        assertNull(result.videoUrl());
        assertEquals("FAILED", result.clipStatus());
    }

    @Test
    @DisplayName("숨김 클립이 있으면 DISABLED를 반환한다")
    void hiddenClipExists_disabled() {
        when(getQtPassageContentContextUseCase.getContentContext(9L))
                .thenReturn(context(9L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip clip = TestEntityFactory.qtVideoClip(
                13L,
                9L,
                sourceVideo,
                "https://cdn.example.com/qt-hidden.mp4",
                QtVideoClipStatus.HIDDEN);
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                9L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of(clip));

        QtVideoClipResponse result = service.getVideo(9L);

        assertEquals("DISABLED", result.status());
        assertEquals(9L, result.qtPassageId());
        assertNull(result.videoUrl());
        assertEquals("HIDDEN", result.clipStatus());
    }

    @Test
    @DisplayName("승인 클립은 실패 또는 숨김 클립보다 우선한다")
    void approvedClipHasPriority() {
        when(getQtPassageContentContextUseCase.getContentContext(10L))
                .thenReturn(context(10L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip hidden = TestEntityFactory.qtVideoClip(
                13L,
                10L,
                sourceVideo,
                "https://cdn.example.com/qt-hidden.mp4",
                QtVideoClipStatus.HIDDEN);
        QtVideoClip approved = TestEntityFactory.qtVideoClip(
                14L,
                10L,
                sourceVideo,
                "https://cdn.example.com/qt-approved.mp4",
                QtVideoClipStatus.APPROVED);
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                10L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of(hidden, approved));

        QtVideoClipResponse result = service.getVideo(10L);

        assertEquals("READY", result.status());
        assertEquals("https://cdn.example.com/qt-approved.mp4", result.videoUrl());
        assertEquals("APPROVED", result.clipStatus());
    }

    @Test
    @DisplayName("숨김 클립은 실패 클립보다 우선한다")
    void hiddenClipHasPriorityOverFailedClip() {
        when(getQtPassageContentContextUseCase.getContentContext(14L))
                .thenReturn(context(14L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip failed = TestEntityFactory.qtVideoClip(
                15L,
                14L,
                sourceVideo,
                "https://cdn.example.com/qt-failed.mp4",
                QtVideoClipStatus.FAILED);
        QtVideoClip hidden = TestEntityFactory.qtVideoClip(
                16L,
                14L,
                sourceVideo,
                "https://cdn.example.com/qt-hidden.mp4",
                QtVideoClipStatus.HIDDEN);
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                14L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of(failed, hidden));

        QtVideoClipResponse result = service.getVideo(14L);

        assertEquals("DISABLED", result.status());
        assertEquals("HIDDEN", result.clipStatus());
    }

    @Test
    @DisplayName("미공개 QT 본문은 차단한다")
    void unpublishedPassage_blocked() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, false));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getVideo(6L));

        assertEquals(ErrorCode.QT_PASSAGE_NOT_FOUND, exception.getErrorCode());
        verify(qtVideoClipRepository, never())
                .findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                        6L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES);
    }

    @Test
    @DisplayName("잘못된 QT 본문 ID는 INVALID_INPUT을 던진다")
    void invalidQtPassageId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getVideo(0L));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("[임시 2026-06-19] 오늘 본문에 영상이 없으면 가장 최근 등록 영상으로 폴백한다")
    void todayMissing_fallsBackToRecentApprovedClip() {
        when(getQtPassageContentContextUseCase.getContentContext(20L)).thenReturn(
                new QtPassageContentContext(20L, LocalDate.of(2026, 6, 19), "today QT", List.of(100L, 101L), true));
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                20L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of());
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip recent = TestEntityFactory.qtVideoClip(
                99L, 15L, sourceVideo, "https://cdn.example.com/qt-2026-06-15.mp4");
        when(qtVideoClipRepository.findTopByStatusAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                QtVideoClipStatus.APPROVED)).thenReturn(Optional.of(recent));

        QtVideoClipResponse result = service.getVideo(20L);

        assertEquals("READY", result.status());
        assertEquals(99L, result.clipId());
        assertEquals("https://cdn.example.com/qt-2026-06-15.mp4", result.videoUrl());
        assertEquals("APPROVED", result.clipStatus());
    }

    @Test
    @DisplayName("[임시 2026-06-19] 오늘이 아닌 본문은 폴백하지 않고 MISSING을 유지한다")
    void nonTodayMissing_noFallback() {
        when(getQtPassageContentContextUseCase.getContentContext(21L)).thenReturn(
                new QtPassageContentContext(21L, LocalDate.of(2026, 6, 10), "past QT", List.of(100L, 101L), true));
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                21L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of());

        QtVideoClipResponse result = service.getVideo(21L);

        assertEquals("MISSING", result.status());
        verify(qtVideoClipRepository, never())
                .findTopByStatusAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(QtVideoClipStatus.APPROVED);
    }

    @Test
    @DisplayName("[임시 2026-06-19] 오늘 본문이지만 최근 등록 영상도 없으면 MISSING을 유지한다")
    void todayMissing_noRecentClip_staysMissing() {
        when(getQtPassageContentContextUseCase.getContentContext(22L)).thenReturn(
                new QtPassageContentContext(22L, LocalDate.of(2026, 6, 19), "today QT", List.of(100L, 101L), true));
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                22L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of());
        when(qtVideoClipRepository.findTopByStatusAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                QtVideoClipStatus.APPROVED)).thenReturn(Optional.empty());

        QtVideoClipResponse result = service.getVideo(22L);

        assertEquals("MISSING", result.status());
        assertEquals(22L, result.qtPassageId());
        assertNull(result.videoUrl());
    }

    private static QtPassageContentContext context(Long qtPassageId, boolean published) {
        return new QtPassageContentContext(
                qtPassageId,
                LocalDate.of(2026, 6, 10),
                "test QT",
                List.of(100L, 101L),
                published
        );
    }
}
