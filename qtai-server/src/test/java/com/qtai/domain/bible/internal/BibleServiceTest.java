package com.qtai.domain.bible.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.qtai.support.TestEntityFactory.bibleBook;
import static com.qtai.support.TestEntityFactory.bibleVerse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibleServiceTest {

    private final BibleBookRepository bibleBookRepository = mock(BibleBookRepository.class);
    private final BibleRepository bibleRepository = mock(BibleRepository.class);
    private final BibleService bibleService = new BibleService(bibleBookRepository, bibleRepository);

    @Test
    @DisplayName("성경 권 목록은 displayOrder 순서의 공개 DTO로 반환한다")
    void listBibleBooks_returnsOrderedPublicDtos() {
        when(bibleBookRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1)));

        List<BibleBookResponse> responses = bibleService.listBibleBooks();

        assertThat(responses).containsExactly(new BibleBookResponse(
                (short) 1,
                "OLD",
                "GEN",
                "창세기",
                "Genesis",
                (short) 1
        ));
    }

    @Test
    @DisplayName("verseFrom만 있으면 단일 절 조회로 처리한다")
    void getVerses_whenOnlyVerseFrom_returnsSingleVerse() {
        BibleBook book = bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1);
        BibleVerse verse = bibleVerse(10L, book, (short) 1, (short) 2);
        when(bibleBookRepository.findByCode("GEN")).thenReturn(Optional.of(book));
        when(bibleRepository.findByBookAndChapterNoAndVerseNoBetweenOrderByVerseNoAsc(
                book, (short) 1, (short) 2, (short) 2
        )).thenReturn(List.of(verse));

        BibleVerseRangeResponse response = bibleService.getVerses("GEN", 1, 2, null);

        assertThat(response.book().code()).isEqualTo("GEN");
        assertThat(response.book().chapter()).isEqualTo(1);
        assertThat(response.verses()).hasSize(1);
        assertThat(response.verses().getFirst().verseNo()).isEqualTo(2);
    }

    @Test
    @DisplayName("지정 범위가 50절을 초과하면 INVALID_INPUT으로 거절한다")
    void getVerses_whenExplicitRangeExceedsLimit_throwsValidationError() {
        assertThatThrownBy(() -> bibleService.getVerses("GEN", 1, 1, 51))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(exception.getMessage()).contains("50 verses");
                });
    }

    @Test
    @DisplayName("verseFrom이 verseTo보다 크면 INVALID_INPUT으로 거절한다")
    void getVerses_whenVerseFromGreaterThanVerseTo_throwsValidationError() {
        assertThatThrownBy(() -> bibleService.getVerses("GEN", 1, 5, 4))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(exception.getMessage()).contains("less than or equal");
                });
    }

    @Test
    @DisplayName("chapter가 1보다 작으면 INVALID_INPUT으로 거절한다")
    void getVerses_whenChapterIsLessThanOne_throwsValidationError() {
        assertThatThrownBy(() -> bibleService.getVerses("GEN", 0, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                    assertThat(exception.getMessage()).contains("chapter");
                });
    }

    @Test
    @DisplayName("존재하지 않는 책은 BIBLE_BOOK_NOT_FOUND로 거절한다")
    void getVerses_whenBookDoesNotExist_throwsBookNotFound() {
        when(bibleBookRepository.findByCode("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bibleService.getVerses("NONE", 1, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BIBLE_BOOK_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않는 절 범위는 BIBLE_VERSE_NOT_FOUND로 거절한다")
    void getVerses_whenVersesDoNotExist_throwsVerseNotFound() {
        BibleBook book = bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1);
        when(bibleBookRepository.findByCode("GEN")).thenReturn(Optional.of(book));
        when(bibleRepository.findByBookAndChapterNoOrderByVerseNoAsc(book, (short) 9))
                .thenReturn(List.of());

        assertThatThrownBy(() -> bibleService.getVerses("GEN", 9, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BIBLE_VERSE_NOT_FOUND));
    }
}
