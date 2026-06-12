package com.qtai.domain.qtvideo.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QtVideoAvailabilityServiceTest {

    @Mock private QtVideoClipRepository qtVideoClipRepository;

    private QtVideoAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new QtVideoAvailabilityService(qtVideoClipRepository);
    }

    @Test
    @DisplayName("APPROVED QT 영상 클립이 있으면 Today QT 버튼 활성 상태로 판단한다")
    void hasReadyVideo_approvedClipExists_true() {
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(7L, QtVideoClipStatus.APPROVED))
                .thenReturn(true);

        assertTrue(service.hasReadyVideo(7L));
    }

    @Test
    @DisplayName("APPROVED QT 영상 클립이 없으면 Today QT 버튼 비활성 상태로 판단한다")
    void hasReadyVideo_approvedClipMissing_false() {
        when(qtVideoClipRepository.existsByQtPassageIdAndStatus(7L, QtVideoClipStatus.APPROVED))
                .thenReturn(false);

        assertFalse(service.hasReadyVideo(7L));
    }

    @Test
    @DisplayName("잘못된 QT 본문 ID는 repository를 호출하지 않고 false로 방어한다")
    void hasReadyVideo_invalidQtPassageId_false() {
        assertFalse(service.hasReadyVideo(null));
        assertFalse(service.hasReadyVideo(0L));

        verifyNoInteractions(qtVideoClipRepository);
    }
}
