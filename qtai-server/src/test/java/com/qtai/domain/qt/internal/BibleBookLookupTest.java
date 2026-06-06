package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BibleBookLookup 단위 테스트(§5.2 #1) — bible api 경유 권 조회.
 */
class BibleBookLookupTest {

    private ListBibleBooksUseCase listBibleBooksUseCase;
    private BibleBookLookup lookup;

    @BeforeEach
    void setUp() {
        listBibleBooksUseCase = mock(ListBibleBooksUseCase.class);
        lookup = new BibleBookLookup(listBibleBooksUseCase);
        when(listBibleBooksUseCase.listBibleBooks()).thenReturn(List.of(
                new BibleBookResponse(1, "OLD", "GEN", "창세기", "Genesis", 1),
                new BibleBookResponse(46, "NEW", "1CO", "고린도전서", "1 Corinthians", 46)));
    }

    @Test
    @DisplayName("영문 권명으로 book id를 정확 일치로 찾는다")
    void findBookIdByEnglishName_found() {
        assertThat(lookup.findBookIdByEnglishName("1 Corinthians")).contains((short) 46);
        assertThat(lookup.findBookIdByEnglishName("Genesis")).contains((short) 1);
    }

    @Test
    @DisplayName("미존재 권명/널은 empty")
    void findBookIdByEnglishName_notFoundOrNull() {
        assertThat(lookup.findBookIdByEnglishName("Unknown Book")).isEmpty();
        assertThat(lookup.findBookIdByEnglishName(null)).isEmpty();
    }

    @Test
    @DisplayName("book id로 권 메타를 찾는다")
    void findById_found() {
        assertThat(lookup.findById((short) 46))
                .map(BibleBookResponse::code)
                .contains("1CO");
        assertThat(lookup.findById((short) 1))
                .map(BibleBookResponse::koreanName)
                .contains("창세기");
    }

    @Test
    @DisplayName("미존재 id/널은 empty")
    void findById_notFoundOrNull() {
        assertThat(lookup.findById((short) 999)).isEmpty();
        assertThat(lookup.findById(null)).isEmpty();
    }
}
