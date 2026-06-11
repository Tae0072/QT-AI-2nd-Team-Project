package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;

class AiGenerationWorkerExecutorContractTest {

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Test
    void generationWorkerJobRequiresPositiveIdsAndRequiredFields() {
        assertThat(validJob().jobId()).isEqualTo(1001L);
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                null,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                1L,
                STARTED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobId must be positive");
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                0L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                1L,
                STARTED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobId must be positive");
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                1001L,
                null,
                AiTargetType.QT_PASSAGE,
                35L,
                1L,
                STARTED_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jobType must not be null");
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                1001L,
                AiGenerationJobType.EXPLANATION,
                null,
                35L,
                1L,
                STARTED_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("targetType must not be null");
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                1001L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                0L,
                1L,
                STARTED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetId must be positive");
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                1001L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                0L,
                STARTED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("promptVersionId must be positive");
        assertThatThrownBy(() -> new AiGenerationWorkerJob(
                1001L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                1L,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("startedAt must not be null");
    }

    @Test
    void generationWorkerResultRequiresAllowedJsonObjectPayloadAndSourceLabel() {
        AiGenerationWorkerResult result = AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                "{\"summary\":\"Allowed worker summary\"}",
                "AI-WORKER"
        );

        assertThat(result.assetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
        assertThat(result.payloadJson()).contains("Allowed worker summary");
        assertThat(result.sourceLabel()).isEqualTo("AI-WORKER");
    }

    @Test
    void generationWorkerResultRejectsInvalidPayloadAndSourceLabel() {
        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                null,
                "{\"summary\":\"Allowed worker summary\"}",
                "AI-WORKER"
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("assetType must not be null");
        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                " ",
                "AI-WORKER"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson must not be blank");
        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                "{not-json",
                "AI-WORKER"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson must be a valid JSON object");
        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                "[{\"summary\":\"Allowed worker summary\"}]",
                "AI-WORKER"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson must be a JSON object");
        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                "{\"summary\":\"Allowed worker summary\"}",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceLabel must not be blank");
        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                "{\"summary\":\"Allowed worker summary\"}",
                " "
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceLabel must not be blank");
    }

    @Test
    void generationWorkerResultRejectsForbiddenPayloadFields() {
        String forbiddenPayload = "{\"" + "prompt" + "\":\"blocked test value\"}";

        assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                AiGeneratedAssetType.EXPLANATION,
                forbiddenPayload,
                "AI-WORKER"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payloadJson must not store forbidden provider or validation reference fields");
    }

    private static AiGenerationWorkerJob validJob() {
        return new AiGenerationWorkerJob(
                1001L,
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                1L,
                STARTED_AT
        );
    }
}
