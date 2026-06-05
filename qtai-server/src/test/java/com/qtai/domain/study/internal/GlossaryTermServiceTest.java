package com.qtai.domain.study.internal;

import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;

import static com.qtai.support.TestEntityFactory.glossaryTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlossaryTermServiceTest {

    private final GlossaryTermRepository glossaryTermRepository = mock(GlossaryTermRepository.class);
    private final GlossaryTermService glossaryTermService = new GlossaryTermService(glossaryTermRepository);

    @Test
    @DisplayName("AI 승인 용어 게시 시 같은 절의 기존 APPROVED 용어를 숨기고 새 APPROVED 용어를 저장한다")
    void publishApprovedGlossaryTerms_hidesExistingApprovedTermsAndSavesNewApprovedTerms() {
        GlossaryTerm existing = glossaryTerm(1L, 10L, GlossaryTermStatus.APPROVED, "old term");
        when(glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(List.of(10L, 11L)))
                .thenReturn(List.of(existing));
        when(glossaryTermRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PublishApprovedGlossaryTermsResult result =
                glossaryTermService.publishApprovedGlossaryTerms(new PublishApprovedGlossaryTermsCommand(
                        500L,
                        "QT-AI DeepSeek",
                        OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                        List.of(
                                new PublishApprovedGlossaryTermsCommand.Term(10L, "new term", "new meaning"),
                                new PublishApprovedGlossaryTermsCommand.Term(11L, "another term", "another meaning")
                        )
                ));

        assertThat(existing.getStatus()).isEqualTo(GlossaryTermStatus.HIDDEN);
        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(result.hiddenCount()).isEqualTo(1);

        ArgumentCaptor<List<GlossaryTerm>> savedCaptor = ArgumentCaptor.captor();
        verify(glossaryTermRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .hasSize(2)
                .allSatisfy(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(GlossaryTermStatus.APPROVED);
                    assertThat(saved.getSourceLabel()).isEqualTo("QT-AI DeepSeek");
                    assertThat(saved.getAiAssetId()).isEqualTo(500L);
                });
        assertThat(savedCaptor.getValue())
                .extracting(GlossaryTerm::getBibleVerseId, GlossaryTerm::getTerm, GlossaryTerm::getMeaning)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "new term", "new meaning"),
                        org.assertj.core.groups.Tuple.tuple(11L, "another term", "another meaning")
                );
        verify(glossaryTermRepository).flush();
    }

    @Test
    @DisplayName("AI 산출물 숨김 시 해당 aiAssetId의 APPROVED 용어를 HIDDEN 처리한다")
    void hidePublishedGlossaryTerms_hidesApprovedTermsLinkedToAiAsset() {
        GlossaryTerm existing = glossaryTerm(1L, 10L, GlossaryTermStatus.APPROVED, "old term");
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of(existing));

        HidePublishedGlossaryTermsResult result =
                glossaryTermService.hidePublishedGlossaryTerms(new HidePublishedGlossaryTermsCommand(500L));

        assertThat(existing.getStatus()).isEqualTo(GlossaryTermStatus.HIDDEN);
        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.hiddenCount()).isEqualTo(1);
        verify(glossaryTermRepository).flush();
    }

    @Test
    @DisplayName("게시할 용어가 없으면 기존 용어를 건드리지 않고 0건 결과를 반환한다")
    void publishApprovedGlossaryTerms_whenTermsEmpty_returnsZeroWithoutReplacingExistingTerms() {
        PublishApprovedGlossaryTermsResult result =
                glossaryTermService.publishApprovedGlossaryTerms(new PublishApprovedGlossaryTermsCommand(
                        500L,
                        "QT-AI DeepSeek",
                        OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                        List.of()
                ));

        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.publishedCount()).isZero();
        assertThat(result.hiddenCount()).isZero();
    }
}
