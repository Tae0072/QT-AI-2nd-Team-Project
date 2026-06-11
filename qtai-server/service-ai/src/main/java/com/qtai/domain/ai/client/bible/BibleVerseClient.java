package com.qtai.domain.ai.client.bible;

import java.util.List;

import com.qtai.domain.ai.client.AiClientException;

public interface BibleVerseClient {

    BibleVerseResult getVerse(Long verseId) throws AiClientException;

    List<BibleVerseResult> getVersesByIds(List<Long> verseIds) throws AiClientException;

    BibleVerseRangeResult getVersesInRange(String bibleBook, int chapter, Integer startVerse, Integer endVerse)
            throws AiClientException;

    record BibleVerseResult(
            Long verseId,
            String bibleBook,
            Integer chapter,
            Integer verse,
            String reference,
            String koreanText,
            String englishText
    ) {
    }

    record BibleVerseRangeResult(
            String bibleBook,
            Integer chapter,
            Integer startVerse,
            Integer endVerse,
            List<BibleVerseResult> verses
    ) {
    }
}
