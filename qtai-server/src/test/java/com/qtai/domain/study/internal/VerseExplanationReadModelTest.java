package com.qtai.domain.study.internal;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static com.qtai.support.TestEntityFactory.verseExplanation;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, VerseExplanationService.class})
@ActiveProfiles("test")
class VerseExplanationReadModelTest {

    @Autowired
    private VerseExplanationRepository verseExplanationRepository;

    @Autowired
    private VerseExplanationService verseExplanationService;

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

    @Test
    @DisplayName("AI 승인 해설 게시 후 같은 절의 ACTIVE 해설은 새 승인본 하나만 조회된다")
    void publishApprovedVerseExplanation_replacesActiveExplanation() {
        Long bibleVerseId = 20L;
        verseExplanationRepository.save(verseExplanation(
                bibleVerseId,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "old active summary"
        ));
        verseExplanationRepository.flush();

        verseExplanationService.publishApprovedVerseExplanation(new PublishApprovedVerseExplanationCommand(
                bibleVerseId,
                "new active summary",
                "new approved explanation",
                "QT-AI DeepSeek",
                500L,
                OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
        ));
        verseExplanationRepository.flush();

        List<VerseExplanation> activeExplanations =
                verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        List.of(bibleVerseId),
                        VerseExplanationStatus.APPROVED,
                        "ACTIVE"
                );

        assertThat(activeExplanations).hasSize(1);
        assertThat(activeExplanations.getFirst().getSummary()).isEqualTo("new active summary");
        assertThat(activeExplanations.getFirst().getAiAssetId()).isEqualTo(500L);
        assertThat(verseExplanationRepository.findAll())
                .filteredOn(explanation -> bibleVerseId.equals(explanation.getBibleVerseId()))
                .hasSize(2);
    }

    @Test
    void hidePublishedVerseExplanation_removesVisibleExplanationAndAllowsRepublish() {
        Long bibleVerseId = 30L;
        verseExplanationService.publishApprovedVerseExplanation(new PublishApprovedVerseExplanationCommand(
                bibleVerseId,
                "first summary",
                "first approved explanation",
                "QT-AI DeepSeek",
                700L,
                OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
        ));
        verseExplanationRepository.flush();

        verseExplanationService.hidePublishedVerseExplanation(new HidePublishedVerseExplanationCommand(700L));
        verseExplanationRepository.flush();

        assertThat(verseExplanationService.listApprovedByVerseIds(List.of(bibleVerseId))).isEmpty();
        assertThat(verseExplanationRepository.findAll())
                .filteredOn(explanation -> Long.valueOf(700L).equals(explanation.getAiAssetId()))
                .singleElement()
                .satisfies(explanation -> {
                    assertThat(explanation.getStatus()).isEqualTo(VerseExplanationStatus.HIDDEN);
                    assertThat(explanation.getActiveUniqueKey()).isNull();
                });

        verseExplanationService.publishApprovedVerseExplanation(new PublishApprovedVerseExplanationCommand(
                bibleVerseId,
                "second summary",
                "second approved explanation",
                "QT-AI DeepSeek",
                701L,
                OffsetDateTime.parse("2026-05-22T10:30:00+09:00")
        ));
        verseExplanationRepository.flush();

        assertThat(verseExplanationService.listApprovedByVerseIds(List.of(bibleVerseId)))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.summary()).isEqualTo("second summary");
                    assertThat(response.aiAssetId()).isEqualTo(701L);
                });
    }
}
