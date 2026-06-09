package com.qtai.domain.study.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;

import static com.qtai.support.TestEntityFactory.glossaryTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GlossaryTermServiceTest {

    private final GlossaryTermRepository glossaryTermRepository = mock(GlossaryTermRepository.class);
    private final GlossaryTermService glossaryTermService = new GlossaryTermService(glossaryTermRepository);

    @Test
    @DisplayName("publishing approved glossary hides overlapping active terms and saves new active terms")
    void publishApprovedGlossaryTerms_hidesExistingApprovedTermsAndSavesNewApprovedTerms() {
        GlossaryTerm existing = glossaryTerm(1L, 10L, GlossaryTermStatus.APPROVED, "old term");
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of());
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
        assertThat(existing.getActiveUniqueKey()).isNull();
        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.publishedCount()).isEqualTo(2);
        assertThat(result.hiddenCount()).isEqualTo(1);

        ArgumentCaptor<List<GlossaryTerm>> savedCaptor = ArgumentCaptor.captor();
        verify(glossaryTermRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .hasSize(2)
                .allSatisfy(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(GlossaryTermStatus.APPROVED);
                    assertThat(saved.getActiveUniqueKey()).isEqualTo(GlossaryTerm.ACTIVE_UNIQUE_KEY);
                    assertThat(saved.getSourceLabel()).isEqualTo("QT-AI DeepSeek");
                    assertThat(saved.getAiAssetId()).isEqualTo(500L);
                });
        assertThat(savedCaptor.getValue())
                .extracting(GlossaryTerm::getBibleVerseId, GlossaryTerm::getTerm, GlossaryTerm::getMeaning)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "new term", "new meaning"),
                        org.assertj.core.groups.Tuple.tuple(11L, "another term", "another meaning")
                );
        verify(glossaryTermRepository, times(2)).flush();
    }

    @Test
    @DisplayName("republishing same AI asset hides previous asset terms not in the new verse set")
    void publishApprovedGlossaryTerms_whenRepublishedSameAiAsset_hidesPreviousAssetTerms() {
        GlossaryTerm previousAssetTerm = glossaryTerm(1L, 9L, GlossaryTermStatus.APPROVED, "previous term");
        GlossaryTerm sameVerseTerm = glossaryTerm(2L, 10L, GlossaryTermStatus.APPROVED, "same verse term");
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of(previousAssetTerm));
        when(glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(List.of(10L)))
                .thenReturn(List.of(sameVerseTerm));
        when(glossaryTermRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PublishApprovedGlossaryTermsResult result =
                glossaryTermService.publishApprovedGlossaryTerms(new PublishApprovedGlossaryTermsCommand(
                        500L,
                        "QT-AI DeepSeek",
                        OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                        List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "new term", "new meaning"))
                ));

        assertThat(previousAssetTerm.getStatus()).isEqualTo(GlossaryTermStatus.HIDDEN);
        assertThat(previousAssetTerm.getActiveUniqueKey()).isNull();
        assertThat(sameVerseTerm.getStatus()).isEqualTo(GlossaryTermStatus.HIDDEN);
        assertThat(sameVerseTerm.getActiveUniqueKey()).isNull();
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.hiddenCount()).isEqualTo(2);
        verify(glossaryTermRepository, times(2)).flush();
    }

    @Test
    @DisplayName("republishing the same approved glossary set is a no-op")
    void publishApprovedGlossaryTerms_whenSameAiAssetAndTermsAlreadyApproved_returnsNoop() {
        GlossaryTerm existing = glossaryTerm(
                1L,
                10L,
                GlossaryTermStatus.APPROVED,
                "same term",
                "same meaning",
                "QT-AI DeepSeek",
                500L
        );
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of(existing));
        when(glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(List.of(10L)))
                .thenReturn(List.of(existing));

        PublishApprovedGlossaryTermsResult result =
                glossaryTermService.publishApprovedGlossaryTerms(new PublishApprovedGlossaryTermsCommand(
                        500L,
                        "QT-AI DeepSeek",
                        OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                        List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "same term", "same meaning"))
                ));

        assertThat(existing.getStatus()).isEqualTo(GlossaryTermStatus.APPROVED);
        assertThat(existing.getActiveUniqueKey()).isEqualTo(GlossaryTerm.ACTIVE_UNIQUE_KEY);
        assertThat(result.publishedCount()).isZero();
        assertThat(result.hiddenCount()).isZero();
        verify(glossaryTermRepository, never()).flush();
        verify(glossaryTermRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("existing terms returned by both lock queries are deduplicated by id")
    void publishApprovedGlossaryTerms_whenSameExistingIdReturnedByBothQueries_hidesOnce() {
        GlossaryTerm existingAssetTerm = glossaryTerm(1L, 10L, GlossaryTermStatus.APPROVED, "old term");
        GlossaryTerm sameIdVerseTerm = glossaryTerm(1L, 10L, GlossaryTermStatus.APPROVED, "old term");
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of(existingAssetTerm));
        when(glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(List.of(10L)))
                .thenReturn(List.of(sameIdVerseTerm));
        when(glossaryTermRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PublishApprovedGlossaryTermsResult result =
                glossaryTermService.publishApprovedGlossaryTerms(new PublishApprovedGlossaryTermsCommand(
                        500L,
                        "QT-AI DeepSeek",
                        OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                        List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "new term", "new meaning"))
                ));

        assertThat(existingAssetTerm.getStatus()).isEqualTo(GlossaryTermStatus.HIDDEN);
        assertThat(existingAssetTerm.getActiveUniqueKey()).isNull();
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.hiddenCount()).isEqualTo(1);
        verify(glossaryTermRepository, times(2)).flush();
    }

    @Test
    @DisplayName("publish converts active unique conflict to business conflict")
    void publishApprovedGlossaryTerms_whenActiveUniqueConflict_throwsConflict() {
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of());
        when(glossaryTermRepository.findApprovedByBibleVerseIdInForUpdate(List.of(10L)))
                .thenReturn(List.of());
        when(glossaryTermRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("duplicate active glossary"))
                .when(glossaryTermRepository).flush();

        assertThatThrownBy(() -> glossaryTermService.publishApprovedGlossaryTerms(
                new PublishApprovedGlossaryTermsCommand(
                        500L,
                        "QT-AI DeepSeek",
                        OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                        List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "term", "meaning"))
                )
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    @DisplayName("hide glossary terms clears active key")
    void hidePublishedGlossaryTerms_hidesApprovedTermsLinkedToAiAsset() {
        GlossaryTerm existing = glossaryTerm(1L, 10L, GlossaryTermStatus.APPROVED, "old term");
        when(glossaryTermRepository.findApprovedByAiAssetIdForUpdate(500L))
                .thenReturn(List.of(existing));

        HidePublishedGlossaryTermsResult result =
                glossaryTermService.hidePublishedGlossaryTerms(new HidePublishedGlossaryTermsCommand(500L));

        assertThat(existing.getStatus()).isEqualTo(GlossaryTermStatus.HIDDEN);
        assertThat(existing.getActiveUniqueKey()).isNull();
        assertThat(result.aiAssetId()).isEqualTo(500L);
        assertThat(result.hiddenCount()).isEqualTo(1);
        verify(glossaryTermRepository).flush();
    }

    @Test
    @DisplayName("publish rejects empty terms")
    void publishApprovedGlossaryTerms_whenTermsEmpty_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                500L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                List.of()
        ));
        verifyNoInteractions(glossaryTermRepository);
    }

    @Test
    @DisplayName("invalid AI asset id is rejected")
    void publishApprovedGlossaryTerms_whenAiAssetIdInvalid_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                0L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "term", "meaning"))
        ));
    }

    @Test
    @DisplayName("null approvedAt is rejected")
    void publishApprovedGlossaryTerms_whenApprovedAtNull_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                500L,
                "QT-AI DeepSeek",
                null,
                List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "term", "meaning"))
        ));
    }

    @Test
    @DisplayName("invalid bibleVerseId is rejected")
    void publishApprovedGlossaryTerms_whenTermBibleVerseIdInvalid_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                500L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                List.of(new PublishApprovedGlossaryTermsCommand.Term(-1L, "term", "meaning"))
        ));
    }

    @Test
    @DisplayName("blank term text is rejected")
    void publishApprovedGlossaryTerms_whenTermTextBlank_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                500L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, " ", "meaning"))
        ));
    }

    @Test
    @DisplayName("blank meaning is rejected")
    void publishApprovedGlossaryTerms_whenMeaningBlank_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                500L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                List.of(new PublishApprovedGlossaryTermsCommand.Term(10L, "term", " "))
        ));
    }

    @Test
    @DisplayName("duplicate bibleVerseId values are rejected before publishing")
    void publishApprovedGlossaryTerms_whenDuplicateBibleVerseId_throwsInvalidInput() {
        assertInvalidInput(new PublishApprovedGlossaryTermsCommand(
                500L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-04T14:20:00+09:00"),
                List.of(
                        new PublishApprovedGlossaryTermsCommand.Term(10L, "same term", "meaning"),
                        new PublishApprovedGlossaryTermsCommand.Term(10L, "other term", "other meaning")
                )
        ));
        verifyNoInteractions(glossaryTermRepository);
    }

    @Test
    @DisplayName("invalid hide AI asset id is rejected")
    void hidePublishedGlossaryTerms_whenAiAssetIdInvalid_throwsInvalidInput() {
        assertThatThrownBy(() -> glossaryTermService.hidePublishedGlossaryTerms(
                new HidePublishedGlossaryTermsCommand(0L)
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private void assertInvalidInput(PublishApprovedGlossaryTermsCommand command) {
        assertThatThrownBy(() -> glossaryTermService.publishApprovedGlossaryTerms(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
