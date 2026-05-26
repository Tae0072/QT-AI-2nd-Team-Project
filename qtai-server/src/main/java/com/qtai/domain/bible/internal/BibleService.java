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
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable("bible-books")
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
        validateChapter(chapter);
        validateVerseRange(verseFrom, verseTo);

        BibleBook book = bibleBookRepository.findByCode(bookCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BIBLE_BOOK_NOT_FOUND));

        List<BibleVerse> verses = findVerses(book, (short) chapter, verseFrom, verseTo);
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

    private void validateChapter(int chapter) {
        if (chapter < 1 || chapter > Short.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateVerseRange(Integer verseFrom, Integer verseTo) {
        if (verseFrom == null && verseTo != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (verseFrom != null && (verseFrom < 1 || verseFrom > Short.MAX_VALUE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (verseTo != null && (verseTo < 1 || verseTo > Short.MAX_VALUE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (verseFrom != null && verseTo != null && verseFrom > verseTo) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (verseFrom != null && verseTo != null
                && verseTo - verseFrom + 1 > MAX_EXPLICIT_RANGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private BibleBookResponse toBookResponse(BibleBook book) {
        return new BibleBookResponse(
                book.getId(),
                book.getTestament().name(),
                book.getCode(),
                book.getKoreanName(),
                book.getEnglishName(),
                book.getDisplayOrder()
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
