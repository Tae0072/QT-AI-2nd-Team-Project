package com.qtai.domain.qt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.qt.api.dto.BiblePassageStudy;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase;
import com.qtai.domain.study.api.dto.QtStudyAvailability;

/**
 * QtService 단위 테스트 — 입력 검증·공개 게이트·캐시 라벨·Note enrich 경로.
 *
 * <p>시간 의존 로직(공개 게이트)을 결정적으로 검증하기 위해 고정 {@link Clock}
 * (2026-06-10 KST)으로 서비스를 직접 생성한다. 외부 의존은 모두 Mockito로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class QtServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 6, 10).atStartOfDay(KST).toInstant(), KST);

    @Mock private QtPassageLookup passageLookup;
    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private QtPassageVerseRepository qtPassageVerseRepository;
    @Mock private TodayQtRangeResolver rangeResolver;
    @Mock private GetNoteUseCase getNoteUseCase;
    @Mock private GetQtStudyAvailabilityUseCase getQtStudyAvailabilityUseCase;
    @Mock private BibleBookLookup bibleBookLookup;

    private QtService qtService;

    @BeforeEach
    void setUp() {
        qtService = new QtService(
                passageLookup, qtPassageRepository, qtPassageVerseRepository,
                rangeResolver, getNoteUseCase, getQtStudyAvailabilityUseCase,
                bibleBookLookup, FIXED_CLOCK);
    }

    private static QtPassage passageOn(LocalDate date) {
        return QtPassage.create(date, (short) 19, (short) 23, (short) 1, (short) 6, "제목", "시 23:1-6");
    }

    private static QtPassage activePassage(Long id, LocalDate date) {
        QtPassage passage = passageOn(date);
        ReflectionTestUtils.setField(passage, "id", id);
        passage.publish(date.atStartOfDay());
        return passage;
    }

    private static QtPassage hiddenPassage(Long id, LocalDate date) {
        QtPassage passage = activePassage(id, date);
        passage.hide(date.atStartOfDay().plusHours(1));
        return passage;
    }

    @Test
    @DisplayName("getPassage — 존재하지 않는 본문이면 QT_PASSAGE_NOT_FOUND")
    void getPassage_없는_본문() {
        when(qtPassageRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.getPassage(1L, 99L));
        assertEquals(ErrorCode.QT_PASSAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getPassage — 미래 본문은 공개 게이트로 숨겨 QT_PASSAGE_NOT_FOUND (CLAUDE.md §6)")
    void getPassage_미래_본문_차단() {
        when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(activePassage(5L, LocalDate.of(2026, 12, 31))));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.getPassage(1L, 5L));
        assertEquals(ErrorCode.QT_PASSAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getPassage — 과거/오늘 본문은 cacheStatus=DIRECT (캐시 미경유)")
    void getPassage_DIRECT_라벨() {
        when(qtPassageRepository.findById(7L)).thenReturn(Optional.of(activePassage(7L, LocalDate.of(2026, 6, 1))));
        lenient().when(getNoteUseCase.getDraft(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new NoteDraftResponse(false, null));

        TodayQtResponse result = qtService.getPassage(1L, 7L);

        assertEquals("DIRECT", result.cacheStatus());
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

    @Test
    @DisplayName("getPassageStudy — 범위를 포함하는 QT 본문에 승인 해설이 있으면 qtPassageId·hasExplanation 노출")
    void getPassageStudy_해설_있음() {
        QtPassage passage = mock(QtPassage.class);
        when(passage.getId()).thenReturn(42L);
        when(passage.getStatus()).thenReturn(QtPassageStatus.ACTIVE);
        when(passage.getQtDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(bibleBookLookup.findBookIdByCode("GEN")).thenReturn(Optional.of((short) 1));
        when(qtPassageRepository.findContainingRange((short) 1, (short) 1, (short) 1, (short) 3))
                .thenReturn(List.of(passage));
        when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(42L))
                .thenReturn(List.of());
        when(getQtStudyAvailabilityUseCase.getAvailability(any(), any()))
                .thenReturn(new QtStudyAvailability("MISSING", true));

        BiblePassageStudy result = qtService.getPassageStudy("GEN", 1, 1, 3);

        assertEquals(42L, result.qtPassageId());
        assertTrue(result.hasExplanation());
    }

    @Test
    @DisplayName("getPassageStudy — 매핑되는 QT 본문이 없으면 NONE")
    void getPassageStudy_매핑_없음() {
        when(bibleBookLookup.findBookIdByCode("GEN")).thenReturn(Optional.of((short) 1));
        when(qtPassageRepository.findContainingRange((short) 1, (short) 50, (short) 1, (short) 1))
                .thenReturn(List.of());

        BiblePassageStudy result = qtService.getPassageStudy("GEN", 50, 1, 1);

        assertNull(result.qtPassageId());
        assertFalse(result.hasExplanation());
    }

    @Test
    @DisplayName("getPassageStudy — 잘못된 입력(범위 역전)은 차단 없이 NONE")
    void getPassageStudy_잘못된_입력() {
        BiblePassageStudy result = qtService.getPassageStudy("GEN", 1, 5, 2);

        assertNull(result.qtPassageId());
        assertFalse(result.hasExplanation());
    }

    @Test
    @DisplayName("getPassageStudy — 권 코드가 없으면(미존재) NONE")
    void getPassageStudy_권_미존재() {
        when(bibleBookLookup.findBookIdByCode("ZZZ")).thenReturn(Optional.empty());

        BiblePassageStudy result = qtService.getPassageStudy("ZZZ", 1, 1, 1);

        assertNull(result.qtPassageId());
        assertFalse(result.hasExplanation());
    }

    @Test
    @DisplayName("getPassageStudy — 매핑된 본문이 있어도 승인 해설이 없으면 NONE(qtPassageId 미노출)")
    void getPassageStudy_해설_없음() {
        QtPassage passage = mock(QtPassage.class);
        when(passage.getId()).thenReturn(7L);
        when(passage.getStatus()).thenReturn(QtPassageStatus.ACTIVE);
        when(passage.getQtDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(bibleBookLookup.findBookIdByCode("GEN")).thenReturn(Optional.of((short) 1));
        when(qtPassageRepository.findContainingRange((short) 1, (short) 1, (short) 1, (short) 3))
                .thenReturn(List.of(passage));
        when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(7L))
                .thenReturn(List.of());
        when(getQtStudyAvailabilityUseCase.getAvailability(any(), any()))
                .thenReturn(new QtStudyAvailability("MISSING", false));

        BiblePassageStudy result = qtService.getPassageStudy("GEN", 1, 1, 3);

        assertNull(result.qtPassageId());
        assertFalse(result.hasExplanation());
    }

    @Test
    @DisplayName("getPassage blocks hidden passages")
    void getPassage_hidden_blocks() {
        when(qtPassageRepository.findById(6L)).thenReturn(Optional.of(hiddenPassage(6L, LocalDate.of(2026, 6, 1))));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qtService.getPassage(1L, 6L));
        assertEquals(ErrorCode.QT_PASSAGE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getContentContext returns hidden passage with published false")
    void getContentContext_hidden_publishedFalse() {
        when(qtPassageRepository.findById(8L))
                .thenReturn(Optional.of(hiddenPassage(8L, LocalDate.of(2026, 6, 1))));
        when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(8L))
                .thenReturn(List.of());

        var result = qtService.getContentContext(8L);

        assertEquals(8L, result.qtPassageId());
        assertFalse(result.published());
    }

    @Test
    @DisplayName("getPassageStudy hides hidden passages")
    void getPassageStudy_hidden_blocks() {
        QtPassage passage = mock(QtPassage.class);
        when(passage.getStatus()).thenReturn(QtPassageStatus.HIDDEN);
        when(bibleBookLookup.findBookIdByCode("GEN")).thenReturn(Optional.of((short) 1));
        when(qtPassageRepository.findContainingRange((short) 1, (short) 1, (short) 1, (short) 3))
                .thenReturn(List.of(passage));

        BiblePassageStudy result = qtService.getPassageStudy("GEN", 1, 1, 3);

        assertNull(result.qtPassageId());
        assertFalse(result.hasExplanation());
    }
}
