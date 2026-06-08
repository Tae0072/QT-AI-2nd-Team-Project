package com.qtai.domain.ai.client.bible;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;

@Component("aiBibleVerseClientMock")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "mock", matchIfMissing = true)
@ConditionalOnMissingBean(BibleVerseClient.class)
public class BibleVerseClientMock implements BibleVerseClient {

    @Override
    public BibleVerseResult getVerse(Long verseId) {
        if (verseId == null) {
            throw validationFailure("verseId must not be null");
        }
        return verse(verseId, 16);
    }

    @Override
    public List<BibleVerseResult> getVersesByIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty() || verseIds.stream().anyMatch(Objects::isNull)) {
            throw validationFailure("verseIds must not be null, empty, or contain null");
        }
        return verseIds.stream()
                .map(verseId -> verse(verseId, verseId.intValue()))
                .toList();
    }

    @Override
    public BibleVerseRangeResult getVersesInRange(String bibleBook, int chapter, Integer startVerse, Integer endVerse) {
        int from = startVerse == null ? 1 : startVerse;
        int to = endVerse == null ? from : endVerse;
        List<BibleVerseResult> verses = IntStream.rangeClosed(from, to)
                .mapToObj(verse -> new BibleVerseResult(
                        (long) verse,
                        bibleBook,
                        chapter,
                        verse,
                        bibleBook + " " + chapter + ":" + verse,
                        "허용된 테스트 본문",
                        "Allowed test verse"
                ))
                .toList();
        return new BibleVerseRangeResult(bibleBook, chapter, from, to, verses);
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, "bible", message);
    }

    private static BibleVerseResult verse(Long verseId, int verse) {
        return new BibleVerseResult(
                verseId,
                "JOHN",
                3,
                verse,
                "JOHN 3:" + verse,
                "허용된 테스트 본문",
                "Allowed test verse"
        );
    }
}
