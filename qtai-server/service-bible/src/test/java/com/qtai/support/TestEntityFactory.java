package com.qtai.support;

import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleVerse;
import com.qtai.domain.qtvideo.internal.QtVideoClip;
import com.qtai.domain.qtvideo.internal.SourceVideo;
import com.qtai.domain.qtvideo.internal.SourceVideoStorageProvider;
import com.qtai.domain.study.internal.GlossaryTerm;
import com.qtai.domain.study.internal.GlossaryTermStatus;
import com.qtai.domain.study.internal.SimulatorClip;
import com.qtai.domain.study.internal.SimulatorClipStatus;
import com.qtai.domain.study.internal.SimulatorComponentLibraryVersion;
import com.qtai.domain.study.internal.VerseExplanation;
import com.qtai.domain.study.internal.VerseExplanationStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public static GlossaryTerm glossaryTerm(
            Long id,
            Long bibleVerseId,
            GlossaryTermStatus status,
            String term
    ) {
        return glossaryTerm(id, bibleVerseId, status, term, "test meaning", "test source", 200L);
    }

    public static GlossaryTerm glossaryTerm(
            Long id,
            Long bibleVerseId,
            GlossaryTermStatus status,
            String term,
            String meaning,
            String sourceLabel,
            Long aiAssetId
    ) {
        GlossaryTerm glossaryTerm = newInstance(GlossaryTerm.class);
        set(glossaryTerm, "id", id);
        set(glossaryTerm, "bibleVerseId", bibleVerseId);
        set(glossaryTerm, "term", term);
        set(glossaryTerm, "meaning", meaning);
        set(glossaryTerm, "sourceLabel", sourceLabel);
        set(glossaryTerm, "status", status);
        set(glossaryTerm, "activeUniqueKey",
                status == GlossaryTermStatus.APPROVED ? GlossaryTerm.ACTIVE_UNIQUE_KEY : null);
        set(glossaryTerm, "aiAssetId", aiAssetId);
        return glossaryTerm;
    }

    public static SimulatorComponentLibraryVersion simulatorComponentLibraryVersion(String version) {
        SimulatorComponentLibraryVersion libraryVersion = newInstance(SimulatorComponentLibraryVersion.class);
        set(libraryVersion, "version", version);
        set(libraryVersion, "status", "ACTIVE");
        return libraryVersion;
    }

    public static SimulatorClip simulatorClip(
            Long id,
            Long qtPassageId,
            SimulatorClipStatus status,
            String sceneScriptJson
    ) {
        SimulatorClip clip = newInstance(SimulatorClip.class);
        set(clip, "id", id);
        set(clip, "qtPassageId", qtPassageId);
        set(clip, "title", "test clip");
        set(clip, "componentLibraryVersion", simulatorComponentLibraryVersion("2026.05.1"));
        set(clip, "sceneScriptJson", sceneScriptJson);
        set(clip, "status", status);
        set(clip, "aiAssetId", 300L);
        return clip;
    }

    public static SourceVideo sourceVideo(Long id, Short bibleBookId, String videoUrl) {
        SourceVideo sourceVideo = SourceVideo.active(
                bibleBookId,
                "test source video",
                SourceVideoStorageProvider.EXTERNAL_URL,
                videoUrl,
                new BigDecimal("600.000"));
        set(sourceVideo, "id", id);
        return sourceVideo;
    }

    public static QtVideoClip qtVideoClip(Long id, Long qtPassageId, SourceVideo sourceVideo, String videoUrl) {
        QtVideoClip clip = QtVideoClip.approvedSingleCut(
                qtPassageId,
                "test QT video",
                sourceVideo,
                videoUrl,
                new BigDecimal("10.000"),
                new BigDecimal("30.000"),
                LocalDateTime.of(2026, 6, 10, 0, 5));
        set(clip, "id", id);
        return clip;
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
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
