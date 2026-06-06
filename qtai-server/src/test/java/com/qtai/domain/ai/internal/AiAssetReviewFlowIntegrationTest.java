package com.qtai.domain.ai.internal;

import static com.qtai.support.TestEntityFactory.verseExplanation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.internal.VerseExplanationRepository;
import com.qtai.domain.study.internal.VerseExplanationService;
import com.qtai.domain.study.internal.VerseExplanationStatus;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class AiAssetReviewFlowIntegrationTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-02T09:00:00+09:00");
    private static final OffsetDateTime REVIEWED_AT = OffsetDateTime.parse("2026-06-02T10:30:00+09:00");

    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;

    @Autowired
    private AiValidationChecklistVersionRepository checklistVersionRepository;

    @Autowired
    private AiValidationLogRepository validationLogRepository;

    @Autowired
    private VerseExplanationRepository verseExplanationRepository;

    private WriteAuditLogUseCase auditLogUseCase;
    private VerseExplanationService verseExplanationService;
    private AiAssetReviewService reviewService;

    @BeforeEach
    void setUp() {
        auditLogUseCase = Mockito.mock(WriteAuditLogUseCase.class);
        verseExplanationService = new VerseExplanationService(verseExplanationRepository);
        reviewService = new AiAssetReviewService(
                generatedAssetRepository,
                validationLogRepository,
                verseExplanationService,
                verseExplanationService,
                auditLogUseCase,
                new ObjectMapper()
        );
    }

    @Test
    void approveExplanationVerseAssetPublishesVisibleVerseExplanation() {
        verseExplanationRepository.save(verseExplanation(
                1001L,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "old active summary"
        ));
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(activeChecklist());
        AiGeneratedAsset asset = generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "new active summary",
                              "explanation": "new active explanation"
                            }
                          ]
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        ));
        saveValidationLog(asset, checklistVersion, 1, AiValidationReviewerType.AUTO, AiValidationResult.PASSED, 10);
        saveValidationLog(asset, checklistVersion, 2, AiValidationReviewerType.ADVISOR, AiValidationResult.PASSED, 12);

        reviewService.reviewAiAsset(new ReviewAiAssetCommand(
                7L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "approved reason",
                true,
                REVIEWED_AT
        ));
        generatedAssetRepository.flush();
        verseExplanationRepository.flush();

        List<ApprovedVerseExplanationResponse> visible =
                verseExplanationService.listApprovedByVerseIds(List.of(1001L));

        assertThat(visible).containsExactly(new ApprovedVerseExplanationResponse(
                1001L,
                "new active summary",
                "new active explanation",
                "QT-AI DeepSeek",
                asset.getId()
        ));
        assertThat(generatedAssetRepository.findById(asset.getId()).orElseThrow().getStatus())
                .isEqualTo(AiGeneratedAssetStatus.APPROVED);
        Mockito.verify(auditLogUseCase).write(Mockito.any(AuditLogWriteRequest.class));
    }

    @Test
    void approveInvalidPublishPayloadKeepsAssetValidatingAndDoesNotCreateVisibleVerseExplanation() {
        verseExplanationRepository.save(verseExplanation(
                1003L,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "old active summary"
        ));
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(activeChecklist());
        AiGeneratedAsset asset = generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1003L,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 2003,
                              "summary": "wrong target summary",
                              "explanation": "wrong target explanation"
                            }
                          ]
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        ));
        saveValidationLog(asset, checklistVersion, 1, AiValidationReviewerType.AUTO, AiValidationResult.PASSED, 10);
        saveValidationLog(asset, checklistVersion, 2, AiValidationReviewerType.ADVISOR, AiValidationResult.PASSED, 12);

        assertThatThrownBy(() -> reviewService.reviewAiAsset(new ReviewAiAssetCommand(
                7L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "approved reason",
                true,
                REVIEWED_AT
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        generatedAssetRepository.flush();
        verseExplanationRepository.flush();

        assertThat(generatedAssetRepository.findById(asset.getId()).orElseThrow().getStatus())
                .isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        assertThat(verseExplanationService.listApprovedByVerseIds(List.of(1003L)))
                .singleElement()
                .satisfies(explanation -> {
                    assertThat(explanation.summary()).isEqualTo("old active summary");
                    assertThat(explanation.aiAssetId()).isNotEqualTo(asset.getId());
                });
        assertThat(verseExplanationRepository.findAll())
                .filteredOn(explanation -> asset.getId().equals(explanation.getAiAssetId()))
                .isEmpty();
        Mockito.verify(auditLogUseCase, Mockito.never()).write(Mockito.any(AuditLogWriteRequest.class));
    }

    @Test
    void hideApprovedExplanationVerseAssetRemovesPublishedVerseExplanationFromVisibleList() {
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(activeChecklist());
        AiGeneratedAsset asset = generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1002L,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1002,
                              "summary": "hide target summary",
                              "explanation": "hide target explanation"
                            }
                          ]
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        ));
        saveValidationLog(asset, checklistVersion, 1, AiValidationReviewerType.AUTO, AiValidationResult.PASSED, 10);
        saveValidationLog(asset, checklistVersion, 2, AiValidationReviewerType.ADVISOR, AiValidationResult.PASSED, 12);
        reviewService.reviewAiAsset(new ReviewAiAssetCommand(
                7L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "approved reason",
                true,
                REVIEWED_AT
        ));
        generatedAssetRepository.flush();
        verseExplanationRepository.flush();

        reviewService.reviewAiAsset(new ReviewAiAssetCommand(
                7L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "HIDE",
                "hide reason",
                false,
                REVIEWED_AT.plusMinutes(5)
        ));
        generatedAssetRepository.flush();
        verseExplanationRepository.flush();

        assertThat(verseExplanationService.listApprovedByVerseIds(List.of(1002L))).isEmpty();
        assertThat(verseExplanationRepository.findAll())
                .filteredOn(explanation -> asset.getId().equals(explanation.getAiAssetId()))
                .singleElement()
                .satisfies(explanation -> {
                    assertThat(explanation.getStatus()).isEqualTo(VerseExplanationStatus.HIDDEN);
                    assertThat(explanation.getActiveUniqueKey()).isNull();
                });
        assertThat(generatedAssetRepository.findById(asset.getId()).orElseThrow().getStatus())
                .isEqualTo(AiGeneratedAssetStatus.HIDDEN);
        Mockito.verify(auditLogUseCase, Mockito.times(2)).write(Mockito.any(AuditLogWriteRequest.class));
    }

    @Test
    void approveRequiresAdvisorLayer2ValidationLog() {
        AiValidationChecklistVersion checklistVersion = checklistVersionRepository.saveAndFlush(activeChecklist());
        AiGeneratedAsset asset = generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1004L,
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1004,
                              "summary": "summary",
                              "explanation": "explanation"
                            }
                          ]
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        ));
        saveValidationLog(asset, checklistVersion, 1, AiValidationReviewerType.AUTO, AiValidationResult.PASSED, 10);

        assertThatThrownBy(() -> reviewService.reviewAiAsset(new ReviewAiAssetCommand(
                7L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "approved reason",
                true,
                REVIEWED_AT
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        generatedAssetRepository.flush();

        assertThat(generatedAssetRepository.findById(asset.getId()).orElseThrow().getStatus())
                .isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        Mockito.verify(auditLogUseCase, Mockito.never()).write(Mockito.any(AuditLogWriteRequest.class));
    }

    private static AiValidationChecklistVersion activeChecklist() {
        AiValidationChecklistVersion version = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.1",
                "hash",
                7L,
                CREATED_AT.minusDays(1)
        );
        version.activate(CREATED_AT.minusHours(1));
        return version;
    }

    private AiValidationLog saveValidationLog(
            AiGeneratedAsset asset,
            AiValidationChecklistVersion checklistVersion,
            int layer,
            AiValidationReviewerType reviewerType,
            AiValidationResult result,
            int createdAtMinutes
    ) {
        return validationLogRepository.saveAndFlush(AiValidationLog.create(
                asset.getId(),
                null,
                layer,
                result,
                reviewerType,
                checklistVersion.getId(),
                "{\"validator\":\"test\"}",
                null,
                CREATED_AT.plusMinutes(createdAtMinutes)
        ));
    }
}
