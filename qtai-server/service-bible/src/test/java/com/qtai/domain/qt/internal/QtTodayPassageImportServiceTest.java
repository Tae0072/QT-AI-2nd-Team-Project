package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QtTodayPassageImportServiceTest {

    @Mock private QtPassageRepository qtPassageRepository;
    @Mock private QtPassageVerseRepository qtPassageVerseRepository;
    @Mock private ListBibleBooksUseCase listBibleBooksUseCase;
    @Mock private GetBibleVerseUseCase getBibleVerseUseCase;
    @Mock private ApplicationEventPublisher eventPublisher;

    private QtTodayPassageImportService service;

    @BeforeEach
    void setUp() {
        service = new QtTodayPassageImportService(
                qtPassageRepository,
                qtPassageVerseRepository,
                listBibleBooksUseCase,
                getBibleVerseUseCase,
                eventPublisher
        );
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(
                new BibleBookResponse(46, "NT", "1CO", "고린도전서", "1 Corinthians", 46)
        ));
        when(qtPassageRepository.findByQtDate(any(LocalDate.class))).thenReturn(java.util.Optional.empty());
        when(qtPassageRepository.save(any(QtPassage.class))).thenAnswer(invocation -> {
            QtPassage passage = invocation.getArgument(0);
            ReflectionTestUtils.setField(passage, "id", 100L);
            return passage;
        });
    }

    @Test
    @DisplayName("장 교차 범위는 시작·종료 장 경계 절을 포함해 순서대로 매핑한다")
    void importToday_mapsCrossChapterBoundaryVersesInOrder() {
        when(getBibleVerseUseCase.getVerses("1CO", 9, null, null)).thenReturn(range(
                verse(901L, 9, 1), verse(902L, 9, 2), verse(903L, 9, 3)
        ));
        when(getBibleVerseUseCase.getVerses("1CO", 10, null, null)).thenReturn(range(
                verse(1001L, 10, 1), verse(1002L, 10, 2), verse(1003L, 10, 3)
        ));

        service.importToday(LocalDate.of(2026, 6, 15), passage((short) 9, (short) 10, (short) 2, (short) 2));

        verify(qtPassageVerseRepository).saveAll(any());
        verify(qtPassageVerseRepository).saveAll(org.mockito.ArgumentMatchers.argThat(mappings -> {
            assertThat(mappings)
                    .extracting(QtPassageVerse::getBibleVerseId)
                    .containsExactly(902L, 903L, 1001L, 1002L);
            assertThat(mappings)
                    .extracting(QtPassageVerse::getDisplayOrder)
                    .containsExactly((short) 1, (short) 2, (short) 3, (short) 4);
            return true;
        }));
    }

    @Test
    @DisplayName("장 교차 범위 중 한 장이 비면(getVerses가 BIBLE_VERSE_NOT_FOUND throw) 기존 매핑을 유지하고 백필 재시도 대상으로 남긴다")
    void importToday_keepsExistingMappingsWhenAnyChapterIsMissing() {
        when(getBibleVerseUseCase.getVerses("1CO", 9, null, null)).thenReturn(range(
                verse(902L, 9, 2), verse(903L, 9, 3)
        ));
        // 실제 계약: 절이 없는 장이면 getVerses는 빈 결과가 아니라 예외를 던진다.
        when(getBibleVerseUseCase.getVerses("1CO", 10, null, null))
                .thenThrow(new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND));

        service.importToday(LocalDate.of(2026, 6, 15), passage((short) 9, (short) 11, (short) 2, (short) 2));

        verify(getBibleVerseUseCase, never()).getVerses("1CO", 11, null, null);
        verify(qtPassageVerseRepository, never()).deleteByQtPassageId(any());
        verify(qtPassageVerseRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any());
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
