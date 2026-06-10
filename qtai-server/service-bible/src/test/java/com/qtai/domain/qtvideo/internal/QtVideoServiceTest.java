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
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class QtVideoServiceTest {

    @Mock private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    @Mock private QtVideoClipRepository qtVideoClipRepository;

    private QtVideoService service;
    private QtVideoAvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        service = new QtVideoService(getQtPassageContentContextUseCase, qtVideoClipRepository);
        availabilityService = new QtVideoAvailabilityService(qtVideoClipRepository);
    }

    @Test
    @DisplayName("approved QT video clip exists -> READY")
    void approvedClipExists_ready() {
        when(getQtPassageContentContextUseCase.getContentContext(4L))
                .thenReturn(context(4L, true));
        SourceVideo sourceVideo = TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4");
        QtVideoClip clip = TestEntityFactory.qtVideoClip(
                10L, 4L, sourceVideo, "https://cdn.example.com/qt-2026-06-17.mp4");
        when(qtVideoClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                4L, QtVideoClipStatus.APPROVED))
                .thenReturn(Optional.of(clip));

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
    @DisplayName("approved QT video clip missing -> MISSING")
    void approvedClipMissing_missing() {
        when(getQtPassageContentContextUseCase.getContentContext(5L))
                .thenReturn(context(5L, true));
        when(qtVideoClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                5L, QtVideoClipStatus.APPROVED))
                .thenReturn(Optional.empty());

        QtVideoClipResponse result = service.getVideo(5L);

        assertEquals("MISSING", result.status());
        assertEquals(5L, result.qtPassageId());
        assertNull(result.videoUrl());
    }

    @Test
    @DisplayName("unpublished QT passage is blocked")
    void unpublishedPassage_blocked() {
        when(getQtPassageContentContextUseCase.getContentContext(6L))
                .thenReturn(context(6L, false));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getVideo(6L));

        assertEquals(ErrorCode.QT_PASSAGE_NOT_FOUND, exception.getErrorCode());
        verify(qtVideoClipRepository, never())
                .findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(6L, QtVideoClipStatus.APPROVED);
    }

    @Test
    @DisplayName("invalid QT passage id throws INVALID_INPUT")
    void invalidQtPassageId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getVideo(0L));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("availability returns READY when approved clip exists")
    void availabilityReady() {
        when(qtVideoClipRepository.findFirstByQtPassageIdAndStatusOrderByApprovedAtDescIdDesc(
                7L, QtVideoClipStatus.APPROVED))
                .thenReturn(Optional.of(TestEntityFactory.qtVideoClip(
                        11L,
                        7L,
                        TestEntityFactory.sourceVideo(1L, (short) 46, "https://cdn.example.com/1co.mp4"),
                        "https://cdn.example.com/qt-2026-06-18.mp4")));

        assertEquals("READY", availabilityService.getAvailability(7L).videoStatus());
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
