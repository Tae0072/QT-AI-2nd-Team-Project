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

import java.util.List;

import static com.qtai.support.TestEntityFactory.bibleBook;
import static com.qtai.support.TestEntityFactory.bibleVerse;
import static com.qtai.support.TestEntityFactory.verseExplanation;
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
        BibleBook book = bibleBookRepository.save(bibleBook((short) 1, "GEN", "창세기", "Genesis", (short) 1));
        BibleVerse verse = bibleRepository.saveAndFlush(bibleVerse(book, (short) 1, (short) 1));

        verseExplanationRepository.save(verseExplanation(
                verse.getId(),
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "visible summary"
        ));
        verseExplanationRepository.save(verseExplanation(
                verse.getId(),
                VerseExplanationStatus.APPROVED,
                null,
                "old summary"
        ));
        verseExplanationRepository.save(verseExplanation(
                verse.getId(),
                VerseExplanationStatus.HIDDEN,
                null,
                "hidden summary"
        ));
        verseExplanationRepository.flush();

        List<VerseExplanation> explanations =
                verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        List.of(verse.getId()),
                        VerseExplanationStatus.APPROVED,
                        "ACTIVE"
                );

        assertThat(explanations).hasSize(1);
        assertThat(explanations.getFirst().getSummary()).isEqualTo("visible summary");
    }
}
