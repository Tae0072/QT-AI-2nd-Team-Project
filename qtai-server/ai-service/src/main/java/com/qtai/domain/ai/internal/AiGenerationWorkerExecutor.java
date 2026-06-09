package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.Objects;

public interface AiGenerationWorkerExecutor {

    AiGenerationWorkerResult execute(AiGenerationWorkerJob job);

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

    record AiGenerationWorkerResult(
            AiGeneratedAssetType assetType,
            String payloadJson,
            String sourceLabel
    ) {

        public AiGenerationWorkerResult {
            Objects.requireNonNull(assetType, "assetType must not be null");
            if (payloadJson == null || payloadJson.isBlank()) {
                throw new IllegalArgumentException("payloadJson must not be blank");
            }
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
}
