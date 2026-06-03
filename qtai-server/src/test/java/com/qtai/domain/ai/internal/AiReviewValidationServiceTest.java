package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.mockito.ArgumentCaptor;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

class AiReviewValidationServiceTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-03T09:00:00+09:00");
    private static final OffsetDateTime VALIDATED_AT = OffsetDateTime.parse("2026-06-03T09:05:00+09:00");
    private static final Long ASSET_ID = 500L;
    private static final Long CHECKLIST_VERSION_ID = 77L;

    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiValidationChecklistVersionRepository checklistVersionRepository;
    private AiValidationLogRepository validationLogRepository;
    private LlmClient llmClient;
    private AiReviewValidationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AiGenerationJobRepository generationJobRepository = mock(AiGenerationJobRepository.class);
        generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
        checklistVersionRepository = mock(AiValidationChecklistVersionRepository.class);
        validationLogRepository = mock(AiValidationLogRepository.class);
        llmClient = mock(LlmClient.class);
        objectMapper = new ObjectMapper();
        AiLogService aiLogService = new AiLogService(
                generationJobRepository,
                generatedAssetRepository,
                validationLogRepository
        );
        service = new AiReviewValidationService(
                generatedAssetRepository,
                checklistVersionRepository,
                aiLogService,
                llmClient,
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
    void passedAdvisorResponseCreatesLayer2AdvisorPassedLog() throws Exception {
        AiGeneratedAsset asset = givenAsset();
        when(llmClient.complete(any(LlmCompletionRequest.class))).thenReturn(completion("""
                {
                  "result": "PASSED",
                  "reason": "출처와 범위가 검수 기준을 충족합니다."
                }
                """));

        AiValidationLog log = service.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getAiAssetId()).isEqualTo(ASSET_ID);
        assertThat(log.getLayer()).isEqualTo(2);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.ADVISOR);
        assertThat(log.getResult()).isEqualTo(AiValidationResult.PASSED);
        assertThat(log.getChecklistVersionId()).isEqualTo(CHECKLIST_VERSION_ID);
        assertThat(log.getErrorMessage()).isNull();
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);

        JsonNode checklistJson = objectMapper.readTree(log.getChecklistJson());
        assertThat(checklistJson.path("validator").asText()).isEqualTo("AI_REVIEW_VALIDATION_LAYER_V2");
        assertThat(checklistJson.path("result").asText()).isEqualTo("PASSED");
        assertThat(checklistJson.path("checklistVersionId").asLong()).isEqualTo(CHECKLIST_VERSION_ID);
        assertThat(log.getChecklistJson()).doesNotContain(
                "providerRawResponse",
                "rawResponse",
                "promptText",
                "validationReferenceText",
                "secret",
                "token",
                "password",
                "privateKey"
        );

        ArgumentCaptor<LlmCompletionRequest> requestCaptor = ArgumentCaptor.forClass(LlmCompletionRequest.class);
        verify(llmClient).complete(requestCaptor.capture());
        assertThat(requestCaptor.getValue().prompt())
                .contains("\"assetId\":500", "\"checklistVersionId\":77")
                .doesNotContain("providerRawResponse", "rawResponse", "secret", "token", "password", "privateKey");
    }

    @Test
    void rejectedAdvisorResponseCreatesLayer2RejectedLogAndRejectsAsset() {
        AiGeneratedAsset asset = givenAsset();
        when(llmClient.complete(any(LlmCompletionRequest.class))).thenReturn(completion("""
                {
                  "result": "REJECTED",
                  "reason": "검수 기준을 충족하지 못했습니다."
                }
                """));

        AiValidationLog log = service.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getLayer()).isEqualTo(2);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.ADVISOR);
        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getErrorMessage()).isEqualTo("AI_REVIEW_REJECTED");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        assertThat(asset.getReviewedAt()).isEqualTo(VALIDATED_AT);
    }

    @Test
    void invalidAdvisorResponseCreatesNeedsReviewLogWithoutRejectingAsset() {
        AiGeneratedAsset asset = givenAsset();
        when(llmClient.complete(any(LlmCompletionRequest.class))).thenReturn(completion("not-json"));

        AiValidationLog log = service.validateExplanationAsset(ASSET_ID, VALIDATED_AT);

        assertThat(log.getResult()).isEqualTo(AiValidationResult.NEEDS_REVIEW);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.ADVISOR);
        assertThat(log.getLayer()).isEqualTo(2);
        assertThat(log.getErrorMessage()).isEqualTo("AI_REVIEW_RESPONSE_INVALID");
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
    }

    @Test
    void nonExplanationAssetIsRejectedBeforeAdvisorCall() {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.SIMULATOR,
                AiTargetType.QT_PASSAGE,
                35L,
                "{}",
                "QT-AI DeepSeek",
                CREATED_AT
        );
        setId(asset, ASSET_ID);
        when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.validateExplanationAsset(ASSET_ID, VALIDATED_AT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private AiGeneratedAsset givenAsset() {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                """
                        {
                          "explanations": [
                            {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                          ],
                          "sourceMetadata": {"verseIds": [1001]}
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        );
        setId(asset, ASSET_ID);
        when(generatedAssetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        return asset;
    }

    private static LlmCompletionResponse completion(String content) {
        return new LlmCompletionResponse(content, 100, 20, 120, "deepseek-chat");
    }

    private static AiValidationChecklistVersion checklistVersion(Long id) {
        AiValidationChecklistVersion version = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.03",
                "sha256:review-checklist",
                null,
                CREATED_AT.minusDays(1)
        );
        version.activate(CREATED_AT.minusHours(1));
        setId(version, id);
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
