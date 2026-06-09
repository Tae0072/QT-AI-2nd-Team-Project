package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;

class DeepSeekGenerationWorkerExecutorSkeletonTest {

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Test
    void constructorRejectsMissingRequiredConfiguration() {
        assertThatThrownBy(() -> new DeepSeekGenerationWorkerExecutor(
                null,
                "deepseek-test-model"
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("deepSeekGenerationClient must not be null");
        assertThatThrownBy(() -> new DeepSeekGenerationWorkerExecutor(
                fakeClient(),
                " "
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qtai.ai.worker.generation.executor.deepseek.model");
    }

    @Test
    void executeFailsSafelyWithoutCallingProvider() {
        DeepSeekGenerationWorkerExecutor executor = new DeepSeekGenerationWorkerExecutor(
                fakeClient(),
                "deepseek-test-model"
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
                fakeClient(),
                "deepseek-test-model"
        );

        assertThatThrownBy(() -> executor.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("job must not be null");
    }

    @Test
    void skeletonKeepsClientAndModelSeparatedFromHttpConfiguration() {
        DeepSeekGenerationClient client = fakeClient();
        DeepSeekGenerationWorkerExecutor executor = new DeepSeekGenerationWorkerExecutor(
                client,
                "deepseek-test-model"
        );

        assertThat(executor.client()).isSameAs(client);
        assertThat(executor.model()).isEqualTo("deepseek-test-model");
    }

    private static DeepSeekGenerationClient fakeClient() {
        return request -> {
            throw new AssertionError("DeepSeek client must not be called by skeleton executor");
        };
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
