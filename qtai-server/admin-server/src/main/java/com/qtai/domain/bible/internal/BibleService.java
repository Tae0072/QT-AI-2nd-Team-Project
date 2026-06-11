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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BibleService implements ListBibleBooksUseCase, GetBibleVerseUseCase {

    private static final int MAX_EXPLICIT_RANGE_SIZE = 50;

    private final BibleBookRepository bibleBookRepository;
    private final BibleRepository bibleRepository;

    // 66권 고정 참조 데이터 — CacheConfig에 등록만 되고 미사용이던 "bibleBooks" 캐시를 실제 사용(P2).
    // qt 범위 해석(BibleBookLookup) 등 반복 호출 비용을 줄인다(24h TTL).
    @Override
    @Cacheable("bibleBooks")
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
    public List<BibleVerseResponse> getVerses(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> requestedIds = new LinkedHashMap<>();
        for (Long verseId : verseIds) {
            if (verseId == null || verseId < 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            requestedIds.putIfAbsent(verseId, verseId);
        }

        Map<Long, BibleVerseResponse> foundVerses = new LinkedHashMap<>();
        bibleRepository.findAllByIdIn(requestedIds.keySet()).stream()
                .map(this::toVerseResponse)
                .forEach(verse -> foundVerses.put(verse.id(), verse));
        if (foundVerses.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND);
        }

        return requestedIds.keySet().stream()
                .map(foundVerses::get)
                .toList();
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
