package com.qtai.domain.study.internal;

import com.qtai.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.qtai.support.TestEntityFactory.verseExplanation;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class VerseExplanationReadModelTest {

    @Autowired
    private VerseExplanationRepository verseExplanationRepository;

    @Test
    @DisplayName("APPROVED이고 activeUniqueKey가 ACTIVE인 해설만 조회한다")
    void findApprovedActiveByBibleVerseIdIn_returnsOnlyApprovedActiveRows() {
        Long bibleVerseId = 10L;

        verseExplanationRepository.save(verseExplanation(
                bibleVerseId,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "visible summary"
        ));
        verseExplanationRepository.save(verseExplanation(
                bibleVerseId,
                VerseExplanationStatus.APPROVED,
                null,
                "old summary"
        ));
        verseExplanationRepository.save(verseExplanation(
                bibleVerseId,
                VerseExplanationStatus.HIDDEN,
                null,
                "hidden summary"
        ));
        verseExplanationRepository.flush();

        List<VerseExplanation> explanations =
                verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        List.of(bibleVerseId),
                        VerseExplanationStatus.APPROVED,
                        "ACTIVE"
                );

        assertThat(explanations).hasSize(1);
        assertThat(explanations.getFirst().getSummary()).isEqualTo("visible summary");
    }
}
