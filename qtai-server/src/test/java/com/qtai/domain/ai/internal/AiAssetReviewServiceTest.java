package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.dto.ReviewAiAssetResult;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.study.api.HidePublishedVerseExplanationUseCase;
import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;

class AiAssetReviewServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-21T09:30:00+09:00");
    private static final OffsetDateTime REVIEWED_AT = OffsetDateTime.parse("2026-05-21T10:30:00+09:00");

    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiValidationChecklistVersionRepository checklistVersionRepository;
    private AiValidationLogRepository validationLogRepository;
    private PublishApprovedVerseExplanationUseCase publishApprovedVerseExplanationUseCase;
    private HidePublishedVerseExplanationUseCase hidePublishedVerseExplanationUseCase;
    private WriteAuditLogUseCase auditLogUseCase;
    private AiAssetReviewService service;

    @BeforeEach
    void setUp() {
        generatedAssetRepository = org.mockito.Mockito.mock(AiGeneratedAssetRepository.class);
        checklistVersionRepository = org.mockito.Mockito.mock(AiValidationChecklistVersionRepository.class);
        validationLogRepository = org.mockito.Mockito.mock(AiValidationLogRepository.class);
        publishApprovedVerseExplanationUseCase =
                org.mockito.Mockito.mock(PublishApprovedVerseExplanationUseCase.class);
        hidePublishedVerseExplanationUseCase =
                org.mockito.Mockito.mock(HidePublishedVerseExplanationUseCase.class);
        auditLogUseCase = org.mockito.Mockito.mock(WriteAuditLogUseCase.class);
        service = new AiAssetReviewService(
                generatedAssetRepository,
                checklistVersionRepository,
                validationLogRepository,
                publishApprovedVerseExplanationUseCase,
                hidePublishedVerseExplanationUseCase,
                auditLogUseCase,
                new ObjectMapper()
        );
    }

    @Test
    void aiAssetReviewServiceImplementsReviewUseCase() {
        assertThat(service).isInstanceOf(ReviewAiAssetUseCase.class);
    }

    @Test
    void approvePassedExplanationVersePublishesVerseExplanationAndWritesSafeAudit() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        AiValidationChecklistVersion checklistVersion = activeChecklist(AiValidationChecklistType.EXPLANATION);
        when(checklistVersionRepository.findById(4L)).thenReturn(Optional.of(checklistVersion));
        when(validationLogRepository.findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(500L, 4L))
                .thenReturn(Optional.of(validationLog(AiValidationResult.PASSED)));

        ReviewAiAssetResult result = service.reviewAiAsset(approveCommand(true));

        assertThat(result.assetId()).isEqualTo(500L);
        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);

        ArgumentCaptor<PublishApprovedVerseExplanationCommand> publishCaptor =
                ArgumentCaptor.forClass(PublishApprovedVerseExplanationCommand.class);
        verify(publishApprovedVerseExplanationUseCase)
                .publishApprovedVerseExplanation(publishCaptor.capture());
        PublishApprovedVerseExplanationCommand publishCommand = publishCaptor.getValue();
        assertThat(publishCommand.bibleVerseId()).isEqualTo(1001L);
        assertThat(publishCommand.summary()).isEqualTo("validated summary");
        assertThat(publishCommand.explanation()).isEqualTo("validated explanation");
        assertThat(publishCommand.sourceLabel()).isEqualTo("QT-AI DeepSeek");
        assertThat(publishCommand.aiAssetId()).isEqualTo(500L);
        assertThat(publishCommand.approvedAt()).isEqualTo(REVIEWED_AT);

        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        AuditLogWriteRequest audit = auditCaptor.getValue();
        assertThat(audit.actorType()).isEqualTo("ADMIN");
        assertThat(audit.actorId()).isEqualTo(7L);
        assertThat(audit.actorLabel()).isEqualTo("ADMIN:7");
        assertThat(audit.actionType()).isEqualTo("AI_ASSET_APPROVE");
        assertThat(audit.targetType()).isEqualTo("AI_GENERATED_ASSET");
        assertThat(audit.targetId()).isEqualTo(500L);
        assertThat(audit.beforeJson()).contains("\"status\":\"VALIDATING\"");
        assertThat(audit.afterJson())
                .contains("\"status\":\"APPROVED\"", "\"activateForTarget\":true")
                .contains("\"reviewedAt\":\"2026-05-21T10:30:00+09:00\"");
        assertThat(audit.beforeJson() + audit.afterJson())
                .doesNotContain(
                        "approved reason",
                        "payloadJson",
                        "validated explanation",
                        "providerRawResponse",
                        "rawResponse",
                        "promptText",
                        "validationReferenceText",
                        "secret",
                        "token",
                        "password",
                        "privateKey"
                );
    }

    @Test
    void approveWithActivateFalseDoesNotPublishVerseExplanation() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L, "not-json");
        stubPassedApproval(asset);

        ReviewAiAssetResult result = service.reviewAiAsset(approveCommand(false));

        assertThat(result.status()).isEqualTo("APPROVED");
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
    }

    @Test
    void approveQtPassageExplanationDoesNotPublishVerseExplanation() {
        AiGeneratedAsset asset = explanationVerseAsset(
                AiTargetType.QT_PASSAGE,
                9001L,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "first summary",
                              "explanation": "first explanation"
                            },
                            {
                              "verseId": 1002,
                              "summary": "second summary",
                              "explanation": "second explanation"
                            }
                          ]
                        }
                        """
        );
        stubPassedApproval(asset);

        ReviewAiAssetResult result = service.reviewAiAsset(approveCommand(true));

        assertThat(result.status()).isEqualTo("APPROVED");
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
    }

    @ParameterizedTest
    @MethodSource("invalidPublishPayloads")
    void approvePublishTargetRejectsInvalidPayloadBeforeStatusTransition(String payloadJson) {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L, payloadJson);
        stubPassedApproval(asset);

        assertThatThrownBy(() -> service.reviewAiAsset(approveCommand(true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void approveRequiresPassedLatestValidationLog() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(checklistVersionRepository.findById(4L))
                .thenReturn(Optional.of(activeChecklist(AiValidationChecklistType.EXPLANATION)));
        when(validationLogRepository.findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(500L, 4L))
                .thenReturn(Optional.of(validationLog(AiValidationResult.NEEDS_REVIEW)));

        assertThatThrownBy(() -> service.reviewAiAsset(approveCommand(true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void approveRejectsRejectedLatestValidationLogWithoutPublishing() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(checklistVersionRepository.findById(4L))
                .thenReturn(Optional.of(activeChecklist(AiValidationChecklistType.EXPLANATION)));
        when(validationLogRepository.findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(500L, 4L))
                .thenReturn(Optional.of(validationLog(AiValidationResult.REJECTED)));

        assertThatThrownBy(() -> service.reviewAiAsset(approveCommand(true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void approveRequiresValidationLogWithoutPublishing() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(checklistVersionRepository.findById(4L))
                .thenReturn(Optional.of(activeChecklist(AiValidationChecklistType.EXPLANATION)));
        when(validationLogRepository.findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(500L, 4L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewAiAsset(approveCommand(true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void approveRequiresValidatingAssetBeforeChecklistAndValidationLogLookup() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        asset.approve(REVIEWED_AT.minusMinutes(1));
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.reviewAiAsset(approveCommand(true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);
        verify(checklistVersionRepository, never()).findById(any());
        verify(validationLogRepository, never())
                .findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(any(), any());
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void approveRequiresActiveMatchingChecklistVersion() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(checklistVersionRepository.findById(4L))
                .thenReturn(Optional.of(retiredChecklist(AiValidationChecklistType.EXPLANATION)));

        assertThatThrownBy(() -> service.reviewAiAsset(approveCommand(true)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(validationLogRepository, never())
                .findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(any(), any());
        verify(auditLogUseCase, never()).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void rejectValidatingAssetWritesRejectAuditOnly() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        ReviewAiAssetResult result = service.reviewAiAsset(command("REJECT", false));

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("AI_ASSET_REJECT");
    }

    @Test
    void hideApprovedExplanationVerseAssetHidesPublishedVerseExplanationAndWritesAudit() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.BIBLE_VERSE, 1001L);
        asset.approve(REVIEWED_AT.minusMinutes(1));
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        ReviewAiAssetResult result = service.reviewAiAsset(command("HIDE", false));

        assertThat(result.status()).isEqualTo("HIDDEN");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.HIDDEN);
        verify(publishApprovedVerseExplanationUseCase, never())
                .publishApprovedVerseExplanation(any(PublishApprovedVerseExplanationCommand.class));
        ArgumentCaptor<HidePublishedVerseExplanationCommand> hideCaptor =
                ArgumentCaptor.forClass(HidePublishedVerseExplanationCommand.class);
        verify(hidePublishedVerseExplanationUseCase).hidePublishedVerseExplanation(hideCaptor.capture());
        assertThat(hideCaptor.getValue().aiAssetId()).isEqualTo(500L);
        ArgumentCaptor<AuditLogWriteRequest> auditCaptor = ArgumentCaptor.forClass(AuditLogWriteRequest.class);
        verify(auditLogUseCase).write(auditCaptor.capture());
        assertThat(auditCaptor.getValue().actionType()).isEqualTo("AI_ASSET_HIDE");
    }

    @Test
    void hideApprovedQtPassageExplanationDoesNotHidePublishedVerseExplanation() {
        AiGeneratedAsset asset = explanationVerseAsset(AiTargetType.QT_PASSAGE, 9001L);
        asset.approve(REVIEWED_AT.minusMinutes(1));
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));

        ReviewAiAssetResult result = service.reviewAiAsset(command("HIDE", false));

        assertThat(result.status()).isEqualTo("HIDDEN");
        verify(hidePublishedVerseExplanationUseCase, never())
                .hidePublishedVerseExplanation(any(HidePublishedVerseExplanationCommand.class));
        verify(auditLogUseCase).write(any(AuditLogWriteRequest.class));
    }

    @Test
    void reviewerAuthorizationIsRequired() {
        assertThatThrownBy(() -> service.reviewAiAsset(new ReviewAiAssetCommand(
                7L,
                500L,
                "USER",
                "REVIEWER",
                "APPROVE",
                4L,
                "approved reason",
                true,
                REVIEWED_AT
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(generatedAssetRepository, never()).findById(any());
    }

    private static ReviewAiAssetCommand approveCommand(boolean activateForTarget) {
        return new ReviewAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                4L,
                "approved reason",
                activateForTarget,
                REVIEWED_AT
        );
    }

    private static ReviewAiAssetCommand command(String action, boolean activateForTarget) {
        return new ReviewAiAssetCommand(
                7L,
                500L,
                "ADMIN",
                "REVIEWER",
                action,
                null,
                "review reason",
                activateForTarget,
                REVIEWED_AT
        );
    }

    private static AiGeneratedAsset explanationVerseAsset(AiTargetType targetType, Long targetId) {
        return explanationVerseAsset(
                targetType,
                targetId,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "validated summary",
                              "explanation": "validated explanation"
                            }
                          ]
                        }
                        """
        );
    }

    private static AiGeneratedAsset explanationVerseAsset(AiTargetType targetType, Long targetId, String payloadJson) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                targetType,
                targetId,
                payloadJson,
                "QT-AI DeepSeek",
                CREATED_AT
        );
        setId(asset, 500L);
        return asset;
    }

    private void stubPassedApproval(AiGeneratedAsset asset) {
        when(generatedAssetRepository.findById(500L)).thenReturn(Optional.of(asset));
        when(checklistVersionRepository.findById(4L))
                .thenReturn(Optional.of(activeChecklist(AiValidationChecklistType.EXPLANATION)));
        when(validationLogRepository.findFirstByAiAssetIdAndChecklistVersionIdOrderByCreatedAtDescIdDesc(500L, 4L))
                .thenReturn(Optional.of(validationLog(AiValidationResult.PASSED)));
    }

    private static Stream<Arguments> invalidPublishPayloads() {
        return Stream.of(
                Arguments.of("not-json"),
                Arguments.of("[]"),
                Arguments.of("""
                        {
                          "summary": "missing explanations"
                        }
                        """),
                Arguments.of("""
                        {
                          "explanations": {
                            "verseId": 1001
                          }
                        }
                        """),
                Arguments.of("""
                        {
                          "explanations": [
                            {
                              "verseId": 2002,
                              "summary": "other summary",
                              "explanation": "other explanation"
                            }
                          ]
                        }
                        """),
                Arguments.of("""
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": " ",
                              "explanation": "validated explanation"
                            }
                          ]
                        }
                        """),
                Arguments.of("""
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": null,
                              "explanation": "validated explanation"
                            }
                          ]
                        }
                        """),
                Arguments.of("""
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "validated summary",
                              "explanation": " "
                            }
                          ]
                        }
                        """),
                Arguments.of("""
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "validated summary",
                              "explanation": null
                            }
                          ]
                        }
                        """)
        );
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

    private static AiValidationChecklistVersion activeChecklist(AiValidationChecklistType checklistType) {
        AiValidationChecklistVersion version = AiValidationChecklistVersion.create(
                checklistType,
                "2026.05.1",
                "hash",
                7L,
                CREATED_AT.minusDays(1)
        );
        setId(version, 4L);
        version.activate(CREATED_AT.minusHours(1));
        return version;
    }

    private static AiValidationChecklistVersion retiredChecklist(AiValidationChecklistType checklistType) {
        AiValidationChecklistVersion version = activeChecklist(checklistType);
        version.retire(CREATED_AT.minusMinutes(30));
        return version;
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
