package com.qtai.domain.study.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.domain.qtvideo.api.GetQtVideoAvailabilityUseCase;
import com.qtai.domain.study.api.dto.QtStudyAvailability;

/**
 * QtStudyAvailabilityService 단위 테스트 — Today QT enrich 가용성 판정.
 *
 * <p>시뮬레이터 READY 기준(승인 클립 존재)과 해설 진입점 기준(승인·ACTIVE 해설 존재)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class QtStudyAvailabilityServiceTest {

    @Mock private GetQtVideoAvailabilityUseCase getQtVideoAvailabilityUseCase;
    @Mock private VerseExplanationRepository verseExplanationRepository;

    @InjectMocks private QtStudyAvailabilityService service;

    @Test
    @DisplayName("qtPassageId가 null이고 verseIds가 비면 MISSING/false (리포지토리 미조회)")
    void 비어있으면_MISSING_false() {
        QtStudyAvailability result = service.getAvailability(null, List.of());

        assertEquals("MISSING", result.simulatorStatus());
        assertFalse(result.hasExplanation());
    }

    @Test
    @DisplayName("승인 클립과 승인 해설이 있으면 READY/true")
    void 승인콘텐츠_있으면_READY_true() {
        when(getQtVideoAvailabilityUseCase.hasReadyVideo(1L)).thenReturn(true);
        when(verseExplanationRepository
                .findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        List.of(10L), VerseExplanationStatus.APPROVED, "ACTIVE"))
                .thenReturn(List.of(new VerseExplanation()));

        QtStudyAvailability result = service.getAvailability(1L, List.of(10L));

        assertEquals("READY", result.simulatorStatus());
        assertTrue(result.hasExplanation());
    }

    @Test
    @DisplayName("승인 클립이 없으면 시뮬레이터 MISSING")
    void 승인클립_없으면_MISSING() {
        lenient().when(getQtVideoAvailabilityUseCase.hasReadyVideo(2L)).thenReturn(false);

        QtStudyAvailability result = service.getAvailability(2L, List.of());

        assertEquals("MISSING", result.simulatorStatus());
        assertFalse(result.hasExplanation());
    }
}
