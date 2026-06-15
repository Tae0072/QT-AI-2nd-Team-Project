package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QtTodayPassageImportServiceTest {

    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private ListBibleBooksUseCase listBibleBooksUseCase;
    @Mock private GetBibleVerseUseCase getBibleVerseUseCase;
    @Mock private QtPassageWriter qtPassageWriter;

    private QtTodayPassageImportService service;

    @BeforeEach
    void setUp() {
        service = new QtTodayPassageImportService(
                qtPassageRepository,
                listBibleBooksUseCase,
                getBibleVerseUseCase,
                qtPassageWriter
        );
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(
                new BibleBookResponse(46, "NT", "1CO", "고린도전서", "1 Corinthians", 46)
        ));
        when(qtPassageWriter.upsert(any(LocalDate.class), any(Short.class), any(SuTodayPassage.class)))
                .thenReturn(savedPassage(100L));
    }

    @Test
    @DisplayName("장 교차 범위는 시작·종료 장 경계 절만 추려 순서대로 매핑 writer에 넘긴다")
    void importToday_passesCrossChapterBoundaryVersesToWriterInOrder() {
        when(getBibleVerseUseCase.getVerses("1CO", 9, null, null)).thenReturn(range(
                verse(901L, 9, 1), verse(902L, 9, 2), verse(903L, 9, 3)
        ));
        when(getBibleVerseUseCase.getVerses("1CO", 10, null, null)).thenReturn(range(
                verse(1001L, 10, 1), verse(1002L, 10, 2), verse(1003L, 10, 3)
        ));

        service.importToday(LocalDate.of(2026, 6, 15), passage((short) 9, (short) 10, (short) 2, (short) 2));

        verify(qtPassageWriter).upsert(any(LocalDate.class), eq((short) 46), any(SuTodayPassage.class));
        verify(qtPassageWriter).replaceMappings(eq(100L), org.mockito.ArgumentMatchers.argThat(verses -> {
            assertThat(verses).extracting(BibleVerseResponse::id).containsExactly(902L, 903L, 1001L, 1002L);
            return true;
        }));
    }

    @Test
    @DisplayName("절 조회가 실패(빈 장 예외)하면 본문만 저장하고 매핑은 호출하지 않으며 예외를 전파하지 않는다")
    void importToday_keepsPassageWhenVerseLookupFails() {
        when(getBibleVerseUseCase.getVerses("1CO", 9, null, null)).thenReturn(range(
                verse(902L, 9, 2), verse(903L, 9, 3)
        ));
        // 실제 계약: 절이 없는 장이면 getVerses는 빈 결과가 아니라 예외를 던진다.
        when(getBibleVerseUseCase.getVerses("1CO", 10, null, null))
                .thenThrow(new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND));

        QtPassage result = service.importToday(
                LocalDate.of(2026, 6, 15), passage((short) 9, (short) 11, (short) 2, (short) 2));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        verify(qtPassageWriter).upsert(any(LocalDate.class), eq((short) 46), any(SuTodayPassage.class));
        verify(qtPassageWriter, never()).replaceMappings(anyLong(), anyList());
    }

    private static QtPassage savedPassage(long id) {
        QtPassage passage = QtPassage.create(
                LocalDate.of(2026, 6, 15), (short) 46, (short) 9, (short) 1, (short) 5, "오늘의 QT", "ref");
        ReflectionTestUtils.setField(passage, "id", id);
        return passage;
    }

    private static SuTodayPassage passage(short chapter, short endChapter, short startVerse, short endVerse) {
        return new SuTodayPassage(
                "오늘의 QT",
                "고린도전서",
                "1 Corinthians",
                chapter,
                endChapter,
                startVerse,
                endVerse,
                "고린도전서(1 Corinthians) " + chapter + ":" + startVerse + "-" + endChapter + ":" + endVerse
        );
    }

    private static BibleVerseRangeResponse range(BibleVerseResponse... verses) {
        return new BibleVerseRangeResponse(null, List.of(verses));
    }

    private static BibleVerseResponse verse(long id, int chapter, int verse) {
        return new BibleVerseResponse(id, "1CO", chapter, verse, "본문", "Text");
    }
}
