package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

@Service
class AiReviewValidationService {

    private static final int REVIEW_VALIDATION_LAYER = 2;
    private static final int MAX_TOKENS = 800;
    private static final double TEMPERATURE = 0.0;
    private static final String REVIEW_VALIDATION_CONFIGURATION_ERROR = "REVIEW_VALIDATION_CONFIGURATION_ERROR";
    private static final String REVIEW_REFERENCE_NOT_FOUND = "AI_REVIEW_REFERENCE_NOT_FOUND";
    private static final String REVIEW_REFERENCE_EXCERPT_NOT_FOUND = "AI_REVIEW_REFERENCE_EXCERPT_NOT_FOUND";
    private static final String VALIDATOR_NAME = "AI_REVIEW_VALIDATION_LAYER_V2";

    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiValidationChecklistVersionRepository checklistVersionRepository;
    private final AiReviewReferenceService reviewReferenceService;
    private final AiReviewReferenceIndexReader referenceIndexReader;
    private final AiReviewReferenceExcerptSelector referenceExcerptSelector;
    private final AiLogService aiLogService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    AiReviewValidationService(
            AiGeneratedAssetRepository generatedAssetRepository,
            AiValidationChecklistVersionRepository checklistVersionRepository,
            AiReviewReferenceService reviewReferenceService,
            AiReviewReferenceIndexReader referenceIndexReader,
            AiReviewReferenceExcerptSelector referenceExcerptSelector,
            AiLogService aiLogService,
            LlmClient llmClient,
            ObjectMapper objectMapper
    ) {
        this.generatedAssetRepository = generatedAssetRepository;
        this.checklistVersionRepository = checklistVersionRepository;
        this.reviewReferenceService = reviewReferenceService;
        this.referenceIndexReader = referenceIndexReader;
        this.referenceExcerptSelector = referenceExcerptSelector;
        this.aiLogService = aiLogService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiValidationLog validateExplanationAsset(Long assetId, OffsetDateTime validatedAt) {
        requirePositive(assetId, "assetId");
        if (validatedAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "validatedAt must not be null");
        }

        AiGeneratedAsset asset = findAsset(assetId);
        requireExplanationAsset(asset);
        AiValidationChecklistVersion checklistVersion = activeExplanationChecklistVersion();
        AiReviewReferenceService.ReferenceMetadata referenceMetadata =
                reviewReferenceService.latestActiveReference().orElse(null);
        ReviewReferenceSelection referenceSelection = referenceSelection(asset, referenceMetadata);
        ReviewOutcome outcome = referenceSelection.canReview()
                ? review(asset, checklistVersion, referenceMetadata, referenceSelection.excerpts())
                : new ReviewOutcome(AiValidationResult.NEEDS_REVIEW, referenceSelection.errorMessage());

        return aiLogService.registerValidationLog(
                asset.getId(),
                referenceMetadata == null ? null : referenceMetadata.validationReferenceJobId(),
                REVIEW_VALIDATION_LAYER,
                outcome.result(),
                AiValidationReviewerType.ADVISOR,
                checklistVersion.getId(),
                checklistJson(checklistVersion, referenceMetadata, referenceSelection.excerpts(), outcome),
                outcome.errorMessage(),
                validatedAt
        );
    }

    private AiGeneratedAsset findAsset(Long assetId) {
        return generatedAssetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));
    }

    private AiValidationChecklistVersion activeExplanationChecklistVersion() {
        List<AiValidationChecklistVersion> activeVersions = checklistVersionRepository.findByChecklistTypeAndStatus(
                AiValidationChecklistType.EXPLANATION,
                AiValidationChecklistStatus.ACTIVE
        );
        if (activeVersions.size() != 1 || activeVersions.get(0).getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, REVIEW_VALIDATION_CONFIGURATION_ERROR);
        }
        return activeVersions.get(0);
    }

    private ReviewOutcome review(
            AiGeneratedAsset asset,
            AiValidationChecklistVersion checklistVersion,
            AiReviewReferenceService.ReferenceMetadata referenceMetadata,
            List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts
    ) {
        try {
            LlmCompletionResponse response = llmClient.complete(new LlmCompletionRequest(
                    null,
                    systemPrompt(),
                    userPrompt(asset, checklistVersion, referenceMetadata, selectedExcerpts),
                    MAX_TOKENS,
                    TEMPERATURE
            ));
            return outcomeFromResponse(response.content());
        } catch (RuntimeException exception) {
            return new ReviewOutcome(AiValidationResult.NEEDS_REVIEW, "AI_REVIEW_RESPONSE_INVALID");
        }
    }

    private ReviewOutcome outcomeFromResponse(String content) {
        try {
            JsonNode root = objectMapper.readTree(requireText(content, "content"));
            if (root == null || !root.isObject()) {
                return needsReview();
            }
            JsonNode resultNode = root.get("result");
            if (resultNode == null || !resultNode.isTextual()) {
                return needsReview();
            }
            return switch (resultNode.asText()) {
                case "PASSED" -> new ReviewOutcome(AiValidationResult.PASSED, null);
                case "REJECTED" -> new ReviewOutcome(AiValidationResult.REJECTED, "AI_REVIEW_REJECTED");
                case "NEEDS_REVIEW" -> new ReviewOutcome(AiValidationResult.NEEDS_REVIEW, "AI_REVIEW_NEEDS_REVIEW");
                default -> needsReview();
            };
        } catch (JsonProcessingException | BusinessException exception) {
            return needsReview();
        }
    }

    private String systemPrompt() {
        return """
                You are QT-AI's final validation advisor.
                Return only a JSON object with result equal to PASSED, REJECTED, or NEEDS_REVIEW.
                Do not include raw provider output, prompt text, secrets, tokens, or long source excerpts.
                """;
    }

    private String userPrompt(
            AiGeneratedAsset asset,
            AiValidationChecklistVersion checklistVersion,
            AiReviewReferenceService.ReferenceMetadata referenceMetadata,
            List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts
    ) {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("assetId", asset.getId());
        prompt.put("assetType", asset.getAssetType().name());
        prompt.put("targetType", asset.getTargetType().name());
        prompt.put("targetId", asset.getTargetId());
        prompt.put("checklistVersionId", checklistVersion.getId());
        prompt.put("checklistType", checklistVersion.getChecklistType().name());
        prompt.put("checklistVersion", checklistVersion.getVersion());
        prompt.put("checklistContentHash", checklistVersion.getContentHash());
        prompt.put("reference", referenceMetadataNode(referenceMetadata, selectedExcerpts));
        prompt.put("assetPayload", payloadNode(asset.getPayloadJson()));
        try {
            return objectMapper.writeValueAsString(prompt);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI_REVIEW_PROMPT_SERIALIZATION_FAILED");
        }
    }

    private Map<String, Object> referenceMetadataNode(
            AiReviewReferenceService.ReferenceMetadata referenceMetadata,
            List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts
    ) {
        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("validationReferenceJobId", referenceMetadata.validationReferenceJobId());
        reference.put("sourceName", referenceMetadata.sourceName());
        reference.put("sourceFileHash", referenceMetadata.sourceFileHash());
        reference.put("indexStorageUri", referenceMetadata.indexStorageUri());
        reference.put("excerpts", selectedExcerpts.stream()
                .map(AiReviewValidationService::referenceExcerptNode)
                .toList());
        return reference;
    }

    private static Map<String, Object> referenceExcerptNode(
            AiReviewReferenceExcerptSelector.SelectedExcerpt selectedExcerpt
    ) {
        Map<String, Object> excerpt = new LinkedHashMap<>();
        excerpt.put("bookCode", selectedExcerpt.bookCode());
        excerpt.put("chapterStart", selectedExcerpt.chapterStart());
        excerpt.put("verseStart", selectedExcerpt.verseStart());
        excerpt.put("chapterEnd", selectedExcerpt.chapterEnd());
        excerpt.put("verseEnd", selectedExcerpt.verseEnd());
        excerpt.put("referenceRangeLabel", selectedExcerpt.referenceRangeLabel());
        excerpt.put("referenceHash", selectedExcerpt.referenceHash());
        excerpt.put("referenceText", selectedExcerpt.referenceText());
        return excerpt;
    }

    private JsonNode payloadNode(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "asset payloadJson is not valid JSON");
        }
    }

    private String checklistJson(
            AiValidationChecklistVersion checklistVersion,
            AiReviewReferenceService.ReferenceMetadata referenceMetadata,
            List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts,
            ReviewOutcome outcome
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("validator", VALIDATOR_NAME);
        root.put("result", outcome.result().name());
        root.put("checklistVersionId", checklistVersion.getId());
        root.put("checklistType", checklistVersion.getChecklistType().name());
        root.put("checklistVersion", checklistVersion.getVersion());
        root.put("checklistContentHash", checklistVersion.getContentHash());
        if (referenceMetadata != null) {
            root.put("validationReferenceJobId", referenceMetadata.validationReferenceJobId());
            root.put("referenceSourceName", referenceMetadata.sourceName());
            root.put("referenceSourceFileHash", referenceMetadata.sourceFileHash());
            root.put("referenceIndexStorageUri", referenceMetadata.indexStorageUri());
        }
        if (!selectedExcerpts.isEmpty()) {
            root.put("selectedReferenceExcerptCount", selectedExcerpts.size());
            ArrayNode hashes = objectMapper.createArrayNode();
            ArrayNode rangeLabels = objectMapper.createArrayNode();
            for (AiReviewReferenceExcerptSelector.SelectedExcerpt selectedExcerpt : selectedExcerpts) {
                hashes.add(selectedExcerpt.referenceHash());
                rangeLabels.add(selectedExcerpt.referenceRangeLabel());
            }
            root.set("selectedReferenceHashes", hashes);
            root.set("selectedReferenceRangeLabels", rangeLabels);
        }
        if (outcome.errorMessage() != null) {
            root.put("errorCode", outcome.errorMessage());
        }
        return root.toString();
    }

    private ReviewReferenceSelection referenceSelection(
            AiGeneratedAsset asset,
            AiReviewReferenceService.ReferenceMetadata referenceMetadata
    ) {
        if (referenceMetadata == null) {
            return ReviewReferenceSelection.blocked(REVIEW_REFERENCE_NOT_FOUND);
        }
        try {
            AiReviewReferenceIndexReader.ReferenceIndex referenceIndex = referenceIndexReader.read(
                    referenceMetadata.indexStorageUri(),
                    referenceMetadata.sourceFileHash()
            );
            List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts =
                    referenceExcerptSelector.select(asset.getPayloadJson(), referenceIndex);
            if (selectedExcerpts.isEmpty()) {
                return ReviewReferenceSelection.blocked(REVIEW_REFERENCE_EXCERPT_NOT_FOUND);
            }
            return ReviewReferenceSelection.ready(selectedExcerpts);
        } catch (BusinessException exception) {
            return ReviewReferenceSelection.blocked(exception.getMessage());
        }
    }

    private void requireExplanationAsset(AiGeneratedAsset asset) {
        if (asset.getAssetType() != AiGeneratedAssetType.EXPLANATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "AI_REVIEW_VALIDATION_EXPLANATION_ONLY");
        }
    }

    private static ReviewOutcome needsReview() {
        return new ReviewOutcome(AiValidationResult.NEEDS_REVIEW, "AI_REVIEW_RESPONSE_INVALID");
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
        return value;
    }

    private record ReviewOutcome(
            AiValidationResult result,
            String errorMessage
    ) {
    }

    private record ReviewReferenceSelection(
            List<AiReviewReferenceExcerptSelector.SelectedExcerpt> excerpts,
            String errorMessage
    ) {

        private static ReviewReferenceSelection ready(
                List<AiReviewReferenceExcerptSelector.SelectedExcerpt> selectedExcerpts
        ) {
            return new ReviewReferenceSelection(List.copyOf(selectedExcerpts), null);
        }

        private static ReviewReferenceSelection blocked(String errorMessage) {
            return new ReviewReferenceSelection(List.of(), errorMessage);
        }

        private boolean canReview() {
            return errorMessage == null;
        }
    }
}
