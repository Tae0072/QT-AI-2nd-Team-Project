package com.qtai.domain.qt.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class AdminQtPassageVerseMapperTest {

    @Mock
    private ListBibleBooksUseCase listBibleBooksUseCase;

    @Mock
    private GetBibleVerseUseCase getBibleVerseUseCase;

    @Mock
    private QtPassageWriter qtPassageWriter;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AdminQtPassageVerseMapper mapper() {
        // TransactionTemplate은 manager.getTransaction()이 null이어도 콜백을 실행하고 commit(null)은 no-op이다.
        return new AdminQtPassageVerseMapper(
                listBibleBooksUseCase, getBibleVerseUseCase, qtPassageWriter, transactionManager);
    }

    private static BibleBookResponse book1Co() {
        return new BibleBookResponse(46, "NEW", "1CO", "고린도전서", "1 Corinthians", 46);
    }

    private static BibleVerseResponse verse(long id, int chapter, int verseNo) {
        return new BibleVerseResponse(id, "1CO", chapter, verseNo, "본문" + verseNo, "text" + verseNo);
    }

    @Test
    @DisplayName("단일 장 범위 — 조회한 절로 qt_passage_verses를 교체 저장한다")
    void mapVerses_singleChapter_replacesMappings() {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(book1Co()));
        List<BibleVerseResponse> verses = List.of(verse(101, 9, 1), verse(102, 9, 2), verse(103, 9, 3));
        when(getBibleVerseUseCase.getVerses("1CO", 9, 1, 3))
                .thenReturn(new BibleVerseRangeResponse(null, verses));

        mapper().mapVerses(500L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 3);

        verify(qtPassageWriter).replaceMappings(eq(500L), eq(verses));
    }

    @Test
    @DisplayName("등록되지 않은 book_id — 매핑을 저장하지 않는다")
    void mapVerses_unknownBook_skips() {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(book1Co()));

        mapper().mapVerses(500L, (short) 1, (short) 1, (short) 1, (short) 1, (short) 5);

        verify(qtPassageWriter, never()).replaceMappings(any(), any());
    }

    @Test
    @DisplayName("조회 결과가 비면 — 매핑을 저장하지 않는다(백필 재시도 대상)")
    void mapVerses_emptyRange_skips() {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(book1Co()));
        when(getBibleVerseUseCase.getVerses("1CO", 9, 1, 3))
                .thenReturn(new BibleVerseRangeResponse(null, List.of()));

        mapper().mapVerses(500L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 3);

        verify(qtPassageWriter, never()).replaceMappings(any(), any());
    }

    @Test
    @DisplayName("bible 조회 예외 — 등록 요청을 깨지 않고 매핑만 보류한다(best-effort)")
    void mapVerses_lookupThrows_isBestEffort() {
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(book1Co()));
        when(getBibleVerseUseCase.getVerses("1CO", 9, 1, 3))
                .thenThrow(new RuntimeException("BIBLE_VERSE_NOT_FOUND"));

        // 예외가 전파되지 않아야 한다.
        mapper().mapVerses(500L, (short) 46, (short) 9, (short) 9, (short) 1, (short) 3);

        verify(qtPassageWriter, never()).replaceMappings(any(), any());
    }
}
