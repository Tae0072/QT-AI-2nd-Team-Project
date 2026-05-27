package com.qtai.domain.bible.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseRangeResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BibleService implements ListBibleBooksUseCase, GetBibleVerseUseCase {

    private static final int MAX_EXPLICIT_RANGE_SIZE = 50;

    private final BibleBookRepository bibleBookRepository;
    private final BibleRepository bibleRepository;

    @Override
    public List<BibleBookResponse> listBibleBooks() {
        return bibleBookRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toBookResponse)
                .toList();
    }

    @Override
    public BibleVerseResponse getVerse(Long verseId) {
        if (verseId == null || verseId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return bibleRepository.findById(verseId)
                .map(this::toVerseResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND));
    }

    @Override
    public BibleVerseRangeResponse getVerses(String bookCode, int chapter, Integer verseFrom, Integer verseTo) {
        short chapterNo = validateChapter(chapter);
        validateVerseRange(verseFrom, verseTo);

        BibleBook book = bibleBookRepository.findByCode(bookCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BIBLE_BOOK_NOT_FOUND));

        List<BibleVerse> verses = findVerses(book, chapterNo, verseFrom, verseTo);
        if (verses.isEmpty()) {
            throw new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND);
        }

        return new BibleVerseRangeResponse(
                new BibleVerseBookResponse(
                        book.getCode(),
                        book.getKoreanName(),
                        book.getEnglishName(),
                        chapter
                ),
                verses.stream()
                        .map(this::toVerseResponse)
                        .toList()
        );
    }

    private List<BibleVerse> findVerses(
            BibleBook book,
            short chapter,
            Integer verseFrom,
            Integer verseTo
    ) {
        if (verseFrom == null) {
            return bibleRepository.findByBookAndChapterNoOrderByVerseNoAsc(book, chapter);
        }

        short from = verseFrom.shortValue();
        short to = (verseTo == null ? verseFrom : verseTo).shortValue();
        return bibleRepository.findByBookAndChapterNoAndVerseNoBetweenOrderByVerseNoAsc(
                book,
                chapter,
                from,
                to
        );
    }

    private short validateChapter(int chapter) {
        if (chapter < 1 || chapter > Short.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "chapter must be between 1 and " + Short.MAX_VALUE);
        }
        return (short) chapter;
    }

    private void validateVerseRange(Integer verseFrom, Integer verseTo) {
        if (verseFrom == null && verseTo != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "verseFrom is required when verseTo is provided");
        }
        if (verseFrom != null && (verseFrom < 1 || verseFrom > Short.MAX_VALUE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "verseFrom must be between 1 and " + Short.MAX_VALUE);
        }
        if (verseTo != null && (verseTo < 1 || verseTo > Short.MAX_VALUE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "verseTo must be between 1 and " + Short.MAX_VALUE);
        }
        if (verseFrom != null && verseTo != null && verseFrom > verseTo) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "verseFrom must be less than or equal to verseTo");
        }
        if (verseFrom != null && verseTo != null
                && verseTo - verseFrom + 1 > MAX_EXPLICIT_RANGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "verse range must not exceed 50 verses");
        }
    }

    private BibleBookResponse toBookResponse(BibleBook book) {
        return new BibleBookResponse(
                Integer.valueOf(book.getId()),
                book.getTestament().name(),
                book.getCode(),
                book.getKoreanName(),
                book.getEnglishName(),
                Integer.valueOf(book.getDisplayOrder())
        );
    }

    private BibleVerseResponse toVerseResponse(BibleVerse verse) {
        return new BibleVerseResponse(
                verse.getId(),
                verse.getBook().getCode(),
                Integer.valueOf(verse.getChapterNo()),
                Integer.valueOf(verse.getVerseNo()),
                verse.getKoreanText(),
                verse.getEnglishText()
        );
    }
}
