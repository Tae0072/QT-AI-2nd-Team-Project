package com.qtai.domain.study.internal;

import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationResult;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static com.qtai.support.TestEntityFactory.verseExplanation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerseExplanationServiceTest {

    private final VerseExplanationRepository verseExplanationRepository = mock(VerseExplanationRepository.class);
    private final VerseExplanationService verseExplanationService =
            new VerseExplanationService(verseExplanationRepository);

    @Test
    @DisplayName("승인된 현재 활성 해설만 공개 DTO로 반환한다")
    void listApprovedByVerseIds_returnsApprovedActiveDtos() {
        when(verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                List.of(10L),
                VerseExplanationStatus.APPROVED,
                "ACTIVE"
        )).thenReturn(List.of(verseExplanation(
                10L,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "summary"
        )));

        List<ApprovedVerseExplanationResponse> responses =
                verseExplanationService.listApprovedByVerseIds(List.of(10L));

        assertThat(responses).containsExactly(new ApprovedVerseExplanationResponse(
                10L,
                "summary",
                "test explanation",
                "test source",
                100L
        ));
    }

    @Test
    @DisplayName("verseIds가 null이면 빈 목록을 반환한다")
    void listApprovedByVerseIds_whenNull_returnsEmptyList() {
        assertThat(verseExplanationService.listApprovedByVerseIds(null)).isEmpty();
    }

    @Test
    @DisplayName("verseIds가 비어 있으면 빈 목록을 반환한다")
    void listApprovedByVerseIds_whenEmpty_returnsEmptyList() {
        assertThat(verseExplanationService.listApprovedByVerseIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("AI 승인 해설 게시 시 기존 ACTIVE 해설을 내리고 새 APPROVED ACTIVE 해설을 저장한다")
    void publishApprovedVerseExplanation_deactivatesExistingActiveAndSavesNewActive() {
        VerseExplanation existing = verseExplanation(
                10L,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "old summary"
        );
        when(verseExplanationRepository.findActiveApprovedByBibleVerseIdForUpdate(10L))
                .thenReturn(List.of(existing));
        when(verseExplanationRepository.save(any(VerseExplanation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PublishApprovedVerseExplanationResult result =
                verseExplanationService.publishApprovedVerseExplanation(new PublishApprovedVerseExplanationCommand(
                        10L,
                        "new summary",
                        "new explanation",
                        "QT-AI DeepSeek",
                        500L,
                        OffsetDateTime.parse("2026-05-21T10:30:00+09:00")
                ));

        assertThat(existing.getActiveUniqueKey()).isNull();
        assertThat(result.bibleVerseId()).isEqualTo(10L);
        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.status()).isEqualTo("APPROVED");

        ArgumentCaptor<VerseExplanation> savedCaptor = ArgumentCaptor.forClass(VerseExplanation.class);
        verify(verseExplanationRepository).save(savedCaptor.capture());
        VerseExplanation saved = savedCaptor.getValue();
        assertThat(saved.getBibleVerseId()).isEqualTo(10L);
        assertThat(saved.getSummary()).isEqualTo("new summary");
        assertThat(saved.getExplanation()).isEqualTo("new explanation");
        assertThat(saved.getSourceLabel()).isEqualTo("QT-AI DeepSeek");
        assertThat(saved.getStatus()).isEqualTo(VerseExplanationStatus.APPROVED);
        assertThat(saved.getActiveUniqueKey()).isEqualTo("ACTIVE");
        assertThat(saved.getAiAssetId()).isEqualTo(500L);
    }

    @Test
    void hidePublishedVerseExplanation_hidesActiveExplanationLinkedToAiAsset() {
        VerseExplanation existing = verseExplanation(
                10L,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "old summary"
        );
        when(verseExplanationRepository.findActiveApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of(existing));

        HidePublishedVerseExplanationResult result =
                verseExplanationService.hidePublishedVerseExplanation(new HidePublishedVerseExplanationCommand(500L));

        assertThat(existing.getStatus()).isEqualTo(VerseExplanationStatus.HIDDEN);
        assertThat(existing.getActiveUniqueKey()).isNull();
        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.hiddenCount()).isEqualTo(1);
        verify(verseExplanationRepository).flush();
    }

    @Test
    void hidePublishedVerseExplanation_whenNoLinkedActiveExplanation_returnsZero() {
        when(verseExplanationRepository.findActiveApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of());

        HidePublishedVerseExplanationResult result =
                verseExplanationService.hidePublishedVerseExplanation(new HidePublishedVerseExplanationCommand(500L));

        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.hiddenCount()).isZero();
    }
}
