package com.qtai.domain.study.internal;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.bible.internal.BibleBook;
import com.qtai.domain.bible.internal.BibleBookRepository;
import com.qtai.domain.bible.internal.BibleRepository;
import com.qtai.domain.bible.internal.BibleVerse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class VerseExplanationReadModelTest {

    @Autowired
    private BibleBookRepository bibleBookRepository;

    @Autowired
    private BibleRepository bibleRepository;

    @Autowired
    private VerseExplanationRepository verseExplanationRepository;

    @Test
    @DisplayName("APPROVED이면서 activeUniqueKey가 ACTIVE인 해설만 사용자 노출 대상으로 조회한다")
    void findApprovedActiveByBibleVerseIdIn_returnsOnlyApprovedActiveRows() {
        BibleBook book = bibleBookRepository.save(book());
        BibleVerse verse = bibleRepository.saveAndFlush(verse(book));

        verseExplanationRepository.save(explanation(verse.getId(), "APPROVED", "ACTIVE", "노출 요약"));
        verseExplanationRepository.save(explanation(verse.getId(), "APPROVED", null, "과거 요약"));
        verseExplanationRepository.save(explanation(verse.getId(), "HIDDEN", null, "숨김 요약"));
        verseExplanationRepository.flush();

        List<VerseExplanation> explanations =
                verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        List.of(verse.getId()),
                        VerseExplanationStatus.APPROVED,
                        "ACTIVE"
                );

        assertThat(explanations).hasSize(1);
        assertThat(explanations.getFirst().getSummary()).isEqualTo("노출 요약");
    }

    private static VerseExplanation explanation(
            Long bibleVerseId,
            String status,
            String activeUniqueKey,
            String summary
    ) {
        VerseExplanation explanation = newInstance(VerseExplanation.class);
        set(explanation, "bibleVerseId", bibleVerseId);
        set(explanation, "summary", summary);
        set(explanation, "explanation", "검증용 해설");
        set(explanation, "sourceLabel", "테스트 출처");
        set(explanation, "status", VerseExplanationStatus.valueOf(status));
        set(explanation, "activeUniqueKey", activeUniqueKey);
        return explanation;
    }

    private static BibleBook book() {
        BibleBook book = newInstance(BibleBook.class);
        set(book, "id", (short) 1);
        set(book, "testament", BibleBook.Testament.OLD);
        set(book, "code", "GEN");
        set(book, "koreanName", "창세기");
        set(book, "englishName", "Genesis");
        set(book, "displayOrder", (short) 1);
        return book;
    }

    private static BibleVerse verse(BibleBook book) {
        BibleVerse verse = newInstance(BibleVerse.class);
        set(verse, "book", book);
        set(verse, "chapterNo", (short) 1);
        set(verse, "verseNo", (short) 1);
        set(verse, "koreanText", "테스트 본문");
        set(verse, "englishText", "Test text");
        return verse;
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
