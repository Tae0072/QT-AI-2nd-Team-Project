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

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class QtVideoServiceTest {

    @Mock private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    @Mock private QtVideoClipRepository qtVideoClipRepository;

    private QtVideoService service;

    @BeforeEach
    void setUp() {
        service = new QtVideoService(getQtPassageContentContextUseCase, qtVideoClipRepository);
    }

    @Test
    @DisplayName("승인된 QT 영상 클립이 있으면 READY를 반환한다")
    void approvedClipExists_ready() {
        when(getQtPassageContentContextUseCase.getContentContext(4L))
                .thenReturn(context(4L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip clip = TestEntityFactory.qtVideoClip(
                10L, 4L, sourceVideo, "https://cdn.example.com/qt-2026-06-17.mp4");
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
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
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
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
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
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
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
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
        when(qtVideoClipRepository.findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
                10L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES))
                .thenReturn(List.of(hidden, approved));

        QtVideoClipResponse result = service.getVideo(10L);

        assertEquals("READY", result.status());
        assertEquals("https://cdn.example.com/qt-approved.mp4", result.videoUrl());
        assertEquals("APPROVED", result.clipStatus());
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
                .findByQtPassageIdAndStatusInOrderByApprovedAtDescIdDesc(
                        6L, QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES);
    }

    @Test
    @DisplayName("잘못된 QT 본문 ID는 INVALID_INPUT을 던진다")
    void invalidQtPassageId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getVideo(0L));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
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
