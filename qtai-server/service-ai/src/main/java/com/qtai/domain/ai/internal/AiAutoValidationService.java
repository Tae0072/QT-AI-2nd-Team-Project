package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Service
class AiAutoValidationService {

    private static final int AUTO_VALIDATION_LAYER = 1;
    private static final String AUTO_VALIDATION_CONFIGURATION_ERROR = "AUTO_VALIDATION_CONFIGURATION_ERROR";
    private static final String VALIDATOR_NAME = "AI_AUTO_VALIDATION_MINIMUM";
    private static final List<String> RULES = List.of(
            "EXPLANATION_SCHEMA",
            "VERSE_SCOPE",
            "FORBIDDEN_FIELDS"
    );
    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "providerRawResponse",
            "provider_raw_response",
            "rawResponse",
            "raw_response",
            "validationReferenceText",
            "validation_reference_text",
            "referenceText",
            "reference_text",
            "promptText",
            "prompt_text",
            "password",
            "secret",
            "token",
            "privateKey",
            "private_key"
    );

    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiValidationChecklistVersionRepository checklistVersionRepository;
    private final AiLogService aiLogService;
    private final AiReviewValidationService aiReviewValidationService;
    private final ObjectMapper objectMapper;

    AiAutoValidationService(
            AiGeneratedAssetRepository generatedAssetRepository,
            AiValidationChecklistVersionRepository checklistVersionRepository,
            AiLogService aiLogService,
            AiReviewValidationService aiReviewValidationService,
            ObjectMapper objectMapper
    ) {
        this.generatedAssetRepository = generatedAssetRepository;
        this.checklistVersionRepository = checklistVersionRepository;
        this.aiLogService = aiLogService;
        this.aiReviewValidationService = aiReviewValidationService;
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
        ValidationOutcome outcome = validatePayload(asset.getPayloadJson());

        AiValidationLog log = aiLogService.registerValidationLog(
                asset.getId(),
                null,
                AUTO_VALIDATION_LAYER,
                outcome.result(),
                AiValidationReviewerType.AUTO,
                checklistVersion.getId(),
                checklistJson(outcome.result()),
                outcome.errorMessage(),
                validatedAt
        );
        if (log.getResult() == AiValidationResult.PASSED) {
            aiReviewValidationService.validateExplanationAsset(asset.getId(), validatedAt);
        }
        return log;
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
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, AUTO_VALIDATION_CONFIGURATION_ERROR);
        }
        return activeVersions.get(0);
    }

    private ValidationOutcome validatePayload(String payloadJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            return rejected("EXPLANATION_PAYLOAD_INVALID_JSON");
        }

        if (root == null || !root.isObject()) {
            return rejected("EXPLANATION_SCHEMA");
        }
        if (containsForbiddenField(root)) {
            return rejected("FORBIDDEN_FIELDS");
        }

        Set<Long> explanationVerseIds = explanationVerseIds(root);
        if (explanationVerseIds.isEmpty()) {
            return rejected("EXPLANATION_SCHEMA");
        }

        Set<Long> sourceVerseIds = sourceVerseIds(root);
        if (sourceVerseIds.isEmpty() || !sourceVerseIds.equals(explanationVerseIds)) {
            return rejected("VERSE_SCOPE");
        }

        return passed();
    }

    private Set<Long> explanationVerseIds(JsonNode root) {
        JsonNode explanations = root.get("explanations");
        if (explanations == null || !explanations.isArray() || explanations.isEmpty()) {
            return Set.of();
        }

        Set<Long> verseIds = new LinkedHashSet<>();
        for (JsonNode explanation : explanations) {
            Long verseId = positiveLong(explanation.get("verseId"));
            if (verseId == null
                    || !verseIds.add(verseId)
                    || !hasNonBlankText(explanation.get("summary"))
                    || !hasNonBlankText(explanation.get("explanation"))) {
                return Set.of();
            }
        }
        return verseIds;
    }

    private Set<Long> sourceVerseIds(JsonNode root) {
        JsonNode sourceMetadata = root.get("sourceMetadata");
        if (sourceMetadata == null || !sourceMetadata.isObject()) {
            return Set.of();
        }

        JsonNode verseIdsNode = sourceMetadata.get("verseIds");
        if (verseIdsNode == null || !verseIdsNode.isArray() || verseIdsNode.isEmpty()) {
            return Set.of();
        }

        Set<Long> verseIds = new LinkedHashSet<>();
        for (JsonNode verseIdNode : verseIdsNode) {
            Long verseId = positiveLong(verseIdNode);
            if (verseId == null || !verseIds.add(verseId)) {
                return Set.of();
            }
        }
        return verseIds;
    }

    private boolean containsForbiddenField(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (FORBIDDEN_FIELD_NAMES.contains(field.getKey()) || containsForbiddenField(field.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                if (containsForbiddenField(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String checklistJson(AiValidationResult result) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("validator", VALIDATOR_NAME);
        root.put("result", result.name());
        ArrayNode rules = objectMapper.createArrayNode();
        RULES.forEach(rules::add);
        root.set("rules", rules);
        return root.toString();
    }

    private void requireExplanationAsset(AiGeneratedAsset asset) {
        if (asset.getAssetType() != AiGeneratedAssetType.EXPLANATION) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "AI_AUTO_VALIDATION_EXPLANATION_ONLY");
        }
    }

    private static ValidationOutcome passed() {
        return new ValidationOutcome(AiValidationResult.PASSED, null);
    }

    private static ValidationOutcome rejected(String errorMessage) {
        return new ValidationOutcome(AiValidationResult.REJECTED, errorMessage);
    }

    private static Long positiveLong(JsonNode node) {
        if (node == null || !node.canConvertToLong()) {
            return null;
        }
        long value = node.asLong();
        if (value <= 0) {
            return null;
        }
        return value;
    }

    private static boolean hasNonBlankText(JsonNode node) {
        return node != null && node.isTextual() && !node.asText().isBlank();
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private record ValidationOutcome(AiValidationResult result, String errorMessage) {
    }
}
