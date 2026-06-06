package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiAutoValidationServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");
    private static final OffsetDateTime VALIDATED_AT = OffsetDateTime.parse("2026-05-29T04:01:00+09:00");
    private static final Long ASSET_ID = 500L;
    private static final Long CHECKLIST_VERSION_ID = 77L;

    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiValidationChecklistVersionRepository checklistVersionRepository;
    private AiValidationLogRepository validationLogRepository;
    private AiReviewValidationService aiReviewValidationService;
    private AiAutoValidationService aiAutoValidationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AiGenerationJobRepository generationJobRepository = mock(AiGenerationJobRepository.class);
        generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
        checklistVersionRepository = mock(AiValidationChecklistVersionRepository.class);
        validationLogRepository = mock(AiValidationLogRepository.class);
        aiReviewValidationService = mock(AiReviewValidationService.class);
        objectMapper = new ObjectMapper();

        AiLogService aiLogService = new AiLogService(
                generationJobRepository,
                generatedAssetRepository,
                validationLogRepository
        );
        aiAutoValidationService = new AiAutoValidationService(
                generatedAssetRepository,
                checklistVersionRepository,
                aiLogService,
                aiReviewValidationService,
                objectMapper
        );

        when(checklistVersionRepository.findByChecklistTypeAndStatus(
                AiValidationChecklistType.EXPLANATION,
                AiValidationChecklistStatus.ACTIVE
        )).thenReturn(List.of(checklistVersion(CHECKLIST_VERSION_ID)));
        when(validationLogRepository.save(any(AiValidationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(generatedAssetRepository.save(any(AiGeneratedAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void validExplanationPayloadCreatesPassedAutoValidationLog() throws Exception {
        AiGeneratedAsset asset = givenAsset("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary one", "explanation": "explanation one"},
                    {"verseId": 1002, "summary": "summary two", "explanation": "explanation two"}
                  ],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001, 1002]}
                }
                """);

        AiValidationLog log = aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.PASSED);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.AUTO);
        assertThat(log.getLayer()).isEqualTo(1);
        assertThat(log.getValidationReferenceJobId()).isNull();
        assertThat(log.getChecklistVersionId()).isEqualTo(CHECKLIST_VERSION_ID);
        assertThat(log.getErrorMessage()).isNull();
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);

        JsonNode checklistJson = objectMapper.readTree(log.getChecklistJson());
        assertThat(checklistJson.path("validator").asText()).isEqualTo("AI_AUTO_VALIDATION_MINIMUM");
        assertThat(checklistJson.path("result").asText()).isEqualTo("PASSED");
        assertThat(checklistJson.path("rules")).hasSize(3);
        assertThat(log.getChecklistJson()).doesNotContain(
                "providerRawResponse",
                "rawResponse",
                "validationReferenceText",
                "promptText"
        );
        verify(aiReviewValidationService).validateExplanationAsset(ASSET_ID, VALIDATED_AT);
    }

    @Test
    void missingExplanationsCreatesRejectedLogAndRejectsAsset() {
        AiGeneratedAsset asset = givenAsset("""
                {
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """);

        AiValidationLog log = aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.AUTO);
        assertThat(log.getErrorMessage()).isEqualTo("EXPLANATION_SCHEMA");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        assertThat(asset.getReviewedAt()).isEqualTo(VALIDATED_AT);
        verify(aiReviewValidationService, never()).validateExplanationAsset(any(), any());
    }

    @Test
    void emptyExplanationsCreatesRejectedLogAndRejectsAsset() {
        AiGeneratedAsset asset = givenAsset("""
                {
                  "explanations": [],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """);

        AiValidationLog log = aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getErrorMessage()).isEqualTo("EXPLANATION_SCHEMA");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
    }

    @Test
    void missingRequiredExplanationFieldCreatesRejectedLogAndRejectsAsset() {
        AiGeneratedAsset asset = givenAsset("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary only"}
                  ],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """);

        AiValidationLog log = aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getErrorMessage()).isEqualTo("EXPLANATION_SCHEMA");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
    }

    @Test
    void sourceVerseIdsMismatchCreatesRejectedLogAndRejectsAsset() {
        AiGeneratedAsset asset = givenAsset("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                  ],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1002]}
                }
                """);

        AiValidationLog log = aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getErrorMessage()).isEqualTo("VERSE_SCOPE");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
    }

    @Test
    void forbiddenPayloadFieldCreatesRejectedLogAndRejectsAsset() {
        AiGeneratedAsset asset = givenAsset("""
                {
                  "explanations": [
                    {
                      "verseId": 1001,
                      "summary": "summary",
                      "explanation": "explanation",
                      "promptText": "must not be stored"
                    }
                  ],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """);

        AiValidationLog log = aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getErrorMessage()).isEqualTo("FORBIDDEN_FIELDS");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
    }

    @Test
    void missingActiveExplanationChecklistThrowsBusinessException() {
        givenAsset("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                  ],
                  "glossaryTerms": [],
                  "sourceMetadata": {"verseIds": [1001]}
                }
                """);
        when(checklistVersionRepository.findByChecklistTypeAndStatus(
                AiValidationChecklistType.EXPLANATION,
                AiValidationChecklistStatus.ACTIVE
        )).thenReturn(List.of());

        assertThatThrownBy(() -> aiAutoValidationService.validateExplanationAsset(ASSET_ID, VALIDATED_AT))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("AUTO_VALIDATION_CONFIGURATION_ERROR");
                });
        verify(validationLogRepository, never()).save(any(AiValidationLog.class));
    }

    private AiGeneratedAsset givenAsset(String payloadJson) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                payloadJson,
                "QT-AI DeepSeek",
                CREATED_AT
        );
        setId(asset, ASSET_ID);
        when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        return asset;
    }

    private static AiValidationChecklistVersion checklistVersion(Long id) {
        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.05.29",
                "sha256:explanation-auto-validation",
                null,
                CREATED_AT.minusDays(1)
        );
        checklistVersion.activate(CREATED_AT.minusHours(1));
        setId(checklistVersion, id);
        return checklistVersion;
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
