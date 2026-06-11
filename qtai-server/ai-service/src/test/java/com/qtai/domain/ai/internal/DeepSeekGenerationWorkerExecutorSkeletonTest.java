package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;

class DeepSeekGenerationWorkerExecutorSkeletonTest {

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Test
    void constructorRejectsMissingRequiredConfiguration() {
        assertThatThrownBy(() -> new DeepSeekGenerationWorkerExecutor(
                " ",
                "redacted-generation-executor-value",
                "deepseek-test-model",
                3000
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qtai.ai.worker.generation.executor.deepseek.base-url");
        assertThatThrownBy(() -> new DeepSeekGenerationWorkerExecutor(
                "http://localhost:65535",
                " ",
                "deepseek-test-model",
                3000
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qtai.ai.worker.generation.executor.deepseek.api-key");
        assertThatThrownBy(() -> new DeepSeekGenerationWorkerExecutor(
                "http://localhost:65535",
                "redacted-generation-executor-value",
                " ",
                3000
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qtai.ai.worker.generation.executor.deepseek.model");
        assertThatThrownBy(() -> new DeepSeekGenerationWorkerExecutor(
                "http://localhost:65535",
                "redacted-generation-executor-value",
                "deepseek-test-model",
                0
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qtai.ai.worker.generation.executor.deepseek.timeout-ms");
    }

    @Test
    void executeFailsSafelyWithoutCallingProvider() {
        DeepSeekGenerationWorkerExecutor executor = new DeepSeekGenerationWorkerExecutor(
                "http://localhost:65535",
                "redacted-generation-executor-value",
                "deepseek-test-model",
                3000
        );

        assertThatThrownBy(() -> executor.execute(validJob()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("DeepSeek generation executor skeleton is not implemented")
                .hasMessageNotContaining("prompt-value")
                .hasMessageNotContaining("provider-response-value")
                .hasMessageNotContaining("reference-body-value")
                .hasMessageNotContaining("credential-value")
                .hasMessageNotContaining("connection-value");
    }

    @Test
    void executeRejectsNullJob() {
        DeepSeekGenerationWorkerExecutor executor = new DeepSeekGenerationWorkerExecutor(
                "http://localhost:65535",
                "redacted-generation-executor-value",
                "deepseek-test-model",
                3000
        );

        assertThatThrownBy(() -> executor.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("job must not be null");
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
