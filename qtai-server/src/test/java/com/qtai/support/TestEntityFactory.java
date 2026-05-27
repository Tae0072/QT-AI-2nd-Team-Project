package com.qtai.support;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleVerse;
import com.qtai.domain.study.internal.VerseExplanation;
import com.qtai.domain.study.internal.VerseExplanationStatus;

import java.lang.reflect.Field;

public final class TestEntityFactory {

    private TestEntityFactory() {
    }

    public static BibleBook bibleBook(
            short id,
            String code,
            String koreanName,
            String englishName,
            short displayOrder
    ) {
        BibleBook book = newInstance(BibleBook.class);
        set(book, "id", id);
        set(book, "testament", BibleBook.Testament.OLD);
        set(book, "code", code);
        set(book, "koreanName", koreanName);
        set(book, "englishName", englishName);
        set(book, "displayOrder", displayOrder);
        return book;
    }

    public static BibleVerse bibleVerse(long id, BibleBook book, short chapterNo, short verseNo) {
        BibleVerse verse = bibleVerse(book, chapterNo, verseNo);
        set(verse, "id", id);
        return verse;
    }

    public static BibleVerse bibleVerse(BibleBook book, short chapterNo, short verseNo) {
        BibleVerse verse = newInstance(BibleVerse.class);
        set(verse, "book", book);
        set(verse, "chapterNo", chapterNo);
        set(verse, "verseNo", verseNo);
        set(verse, "koreanText", "test korean body");
        set(verse, "englishText", "test english body");
        return verse;
    }

    public static VerseExplanation verseExplanation(
            Long bibleVerseId,
            VerseExplanationStatus status,
            String activeUniqueKey,
            String summary
    ) {
        VerseExplanation explanation = newInstance(VerseExplanation.class);
        set(explanation, "bibleVerseId", bibleVerseId);
        set(explanation, "summary", summary);
        set(explanation, "explanation", "test explanation");
        set(explanation, "sourceLabel", "test source");
        set(explanation, "status", status);
        set(explanation, "activeUniqueKey", activeUniqueKey);
        set(explanation, "aiAssetId", 100L);
        return explanation;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
