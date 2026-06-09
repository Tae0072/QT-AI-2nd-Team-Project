package com.qtai.domain.qt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase;

/**
 * QtService 단위 테스트 — 입력 검증·공개 게이트·Note enrich 경로.
 *
 * <p>외부 의존(Repository·Lookup·Note/Study UseCase·Clock)은 모두 Mockito로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class QtServiceTest {

    @Mock private QtPassageLookup passageLookup;
    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private QtPassageVerseRepository qtPassageVerseRepository;
    @Mock private TodayQtRangeResolver rangeResolver;
    @Mock private GetNoteUseCase getNoteUseCase;
    @Mock private GetQtStudyAvailabilityUseCase getQtStudyAvailabilityUseCase;
    @Mock private Clock clock;

    @InjectMocks private QtService qtService;

    @Test
    @DisplayName("getPassage — 존재하지 않는 본문이면 QT_PASSAGE_NOT_FOUND")
    void getPassage_없는_본문() {
        when(qtPassageRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.getPassage(1L, 99L));
        assertEquals(ErrorCode.QT_PASSAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getContentContext — id가 null이면 INVALID_INPUT")
    void getContentContext_null() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.getContentContext(null));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("getContentContext — id가 1 미만이면 INVALID_INPUT")
    void getContentContext_0() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.getContentContext(0L));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("findContentContextByDate — 날짜가 null이면 INVALID_INPUT")
    void findContentContextByDate_null() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.findContentContextByDate(null));
        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    @DisplayName("getToday — 본문이 없으면(EMPTY) draftNoteId/qtPassageId 없이 그대로 반환")
    void getToday_빈_본문() {
        TodayQtResponse empty =
                new TodayQtResponse(null, null, null, "MISSING", false, null, "EMPTY");
        when(passageLookup.findTodayPassage()).thenReturn(empty);

        TodayQtResponse result = qtService.getToday(123L);

        assertNull(result.qtPassageId());
        assertNull(result.draftNoteId());
        assertEquals("EMPTY", result.cacheStatus());
    }
}
