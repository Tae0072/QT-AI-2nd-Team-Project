package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.study.api.HidePublishedGlossaryTermsUseCase;
import com.qtai.domain.study.api.HidePublishedVerseExplanationUseCase;
import com.qtai.domain.study.api.PublishApprovedGlossaryTermsUseCase;
import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiAssetReviewServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-11T09:00:00+09:00");
    private static final OffsetDateTime REVIEWED_AT = OffsetDateTime.parse("2026-06-11T10:00:00+09:00");

    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiValidationLogRepository validationLogRepository;
    private PublishApprovedVerseExplanationUseCase publishExplanationUseCase;
    private HidePublishedVerseExplanationUseCase hideExplanationUseCase;
    private PublishApprovedGlossaryTermsUseCase publishGlossaryUseCase;
    private HidePublishedGlossaryTermsUseCase hideGlossaryUseCase;
    private AiAssetReviewService service;

    @BeforeEach
    void setUp() {
        generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
        validationLogRepository = mock(AiValidationLogRepository.class);
        publishExplanationUseCase = mock(PublishApprovedVerseExplanationUseCase.class);
        hideExplanationUseCase = mock(HidePublishedVerseExplanationUseCase.class);
        publishGlossaryUseCase = mock(PublishApprovedGlossaryTermsUseCase.class);
        hideGlossaryUseCase = mock(HidePublishedGlossaryTermsUseCase.class);
        service = new AiAssetReviewService(
                generatedAssetRepository,
                validationLogRepository,
                publishExplanationUseCase,
                hideExplanationUseCase,
                publishGlossaryUseCase,
                hideGlossaryUseCase,
                mock(WriteAuditLogUseCase.class),
                new ObjectMapper()
        );
    }

    @Test
    void approvePublishesVerseExplanationAndGlossaryTerms() {
        AiGeneratedAsset asset = asset();
        stubPassedApproval(asset);

        service.reviewAiAsset(command("APPROVE", true));

        verify(publishExplanationUseCase)
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        ArgumentCaptor<PublishApprovedGlossaryTermsCommand> glossaryCaptor =
                ArgumentCaptor.forClass(PublishApprovedGlossaryTermsCommand.class);
        verify(publishGlossaryUseCase).publishApprovedGlossaryTerms(glossaryCaptor.capture());
        assertThat(glossaryCaptor.getValue().aiAssetId()).isEqualTo(500L);
        assertThat(glossaryCaptor.getValue().terms()).hasSize(1);
        assertThat(glossaryCaptor.getValue().terms().get(0).term()).isEqualTo("validated term");
    }

    @Test
    void approveAllowsOptionalReasonAndSeparatesAssetFromValidating() {
        AiGeneratedAsset asset = asset();
        stubPassedApproval(asset);

        service.reviewAiAsset(command("APPROVE", false, null));

        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);
    }

    @Test
    void hideHidesVerseExplanationAndGlossaryTerms() {
        AiGeneratedAsset asset = asset();
        asset.approve(REVIEWED_AT.minusMinutes(1));
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        service.reviewAiAsset(command("HIDE", false));

        verify(hideExplanationUseCase).hidePublishedVerseExplanation(any(HidePublishedVerseExplanationCommand.class));
        ArgumentCaptor<HidePublishedGlossaryTermsCommand> glossaryCaptor =
                ArgumentCaptor.forClass(HidePublishedGlossaryTermsCommand.class);
        verify(hideGlossaryUseCase).hidePublishedGlossaryTerms(glossaryCaptor.capture());
        assertThat(glossaryCaptor.getValue().aiAssetId()).isEqualTo(500L);
        verify(publishGlossaryUseCase, never()).publishApprovedGlossaryTerms(any());
    }

    @Test
    void rejectAllowsOptionalReasonAndSeparatesAssetFromValidating() {
        AiGeneratedAsset asset = asset();
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        service.reviewAiAsset(command("REJECT", false, null));

        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        verify(publishExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        verify(publishGlossaryUseCase, never())
                .publishApprovedGlossaryTerms(any(PublishApprovedGlossaryTermsCommand.class));
    }

    private void stubPassedApproval(AiGeneratedAsset asset) {
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(validationLogRepository.findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc(
                500L,
                1,
                AiValidationReviewerType.AUTO
        )).thenReturn(Optional.of(validationLog(AiValidationResult.PASSED)));
        when(validationLogRepository.findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc(
                500L,
                2,
                AiValidationReviewerType.ADVISOR
        )).thenReturn(Optional.of(validationLog(AiValidationResult.PASSED)));
    }

    private static ReviewAiAssetCommand command(String action, boolean activateForTarget) {
        return command(action, activateForTarget, "review reason");
    }

    private static ReviewAiAssetCommand command(String action, boolean activateForTarget, String reason) {
        return new ReviewAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "REVIEWER",
                action,
                reason,
                activateForTarget,
                REVIEWED_AT
        );
    }

    private static AiGeneratedAsset asset() {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "validated summary",
                              "explanation": "validated explanation"
                            }
                          ],
                          "glossaryTerms": [
                            {
                              "verseId": 1001,
                              "term": "validated term",
                              "meaning": "validated meaning"
                            }
                          ]
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        );
        setId(asset, 500L);
        return asset;
    }

    private static AiValidationLog validationLog(AiValidationResult result) {
        return AiValidationLog.create(
                500L,
                null,
                1,
                result,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"validator\":\"test\"}",
                null,
                REVIEWED_AT.minusMinutes(10)
        );
    }

    private static void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
