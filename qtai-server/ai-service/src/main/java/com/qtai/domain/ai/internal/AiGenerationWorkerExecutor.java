package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface AiGenerationWorkerExecutor {

    /**
     * Executes a claimed generation job. Implementations must not store prompt text,
     * provider raw responses, scripture text, credentials, or DB connection values
     * in the returned payload.
     */
    AiGenerationWorkerResult execute(AiGenerationWorkerJob job);

    /**
     * Immutable snapshot of a claimed generation job handed to an executor.
     */
    record AiGenerationWorkerJob(
            Long jobId,
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            OffsetDateTime startedAt
    ) {

        public AiGenerationWorkerJob {
            requirePositive(jobId, "jobId");
            Objects.requireNonNull(jobType, "jobType must not be null");
            Objects.requireNonNull(targetType, "targetType must not be null");
            requirePositive(targetId, "targetId");
            requirePositive(promptVersionId, "promptVersionId");
            Objects.requireNonNull(startedAt, "startedAt must not be null");
        }
    }

    /**
     * Executor output that can be persisted as an AI generated asset.
     * payloadJson must be a JSON object containing only allowed result fields.
     */
    record AiGenerationWorkerResult(
            AiGeneratedAssetType assetType,
            String payloadJson,
            String sourceLabel
    ) {

        public AiGenerationWorkerResult {
            Objects.requireNonNull(assetType, "assetType must not be null");
            payloadJson = requireJsonObjectPayload(payloadJson);
            sourceLabel = requireText(sourceLabel, "sourceLabel");
        }

        public static AiGenerationWorkerResult of(
                AiGeneratedAssetType assetType,
                String payloadJson,
                String sourceLabel
        ) {
            return new AiGenerationWorkerResult(assetType, payloadJson, sourceLabel);
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private static String requireJsonObjectPayload(String payloadJson) {
        String checkedPayloadJson = requireText(payloadJson, "payloadJson");
        checkedPayloadJson = AiJsonStorageGuard.rejectRawProviderOrReferenceText(
                checkedPayloadJson,
                "payloadJson"
        );
        try {
            JsonNode payloadNode = new ObjectMapper().readTree(checkedPayloadJson);
            if (payloadNode == null || !payloadNode.isObject()) {
                throw new IllegalArgumentException("payloadJson must be a JSON object");
            }
            return checkedPayloadJson;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("payloadJson must be a valid JSON object", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
