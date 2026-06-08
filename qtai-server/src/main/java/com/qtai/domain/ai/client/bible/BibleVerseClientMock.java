package com.qtai.domain.ai.client.bible;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("aiBibleVerseClientMock")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "qtai.ai.client.mock.enabled", havingValue = "true")
@ConditionalOnMissingBean(BibleVerseClient.class)
public class BibleVerseClientMock implements BibleVerseClient {

    @Override
    public BibleVerseResult getVerse(Long verseId) {
        return verse(verseId, 16);
    }

    @Override
    public List<BibleVerseResult> getVerses(List<Long> verseIds) {
        return verseIds.stream()
                .map(verseId -> verse(verseId, verseId.intValue()))
                .toList();
    }

    @Override
    public BibleVerseRangeResult getVerses(String bibleBook, int chapter, Integer startVerse, Integer endVerse) {
        int from = startVerse == null ? 1 : startVerse;
        int to = endVerse == null ? from : endVerse;
        List<BibleVerseResult> verses = java.util.stream.IntStream.rangeClosed(from, to)
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
