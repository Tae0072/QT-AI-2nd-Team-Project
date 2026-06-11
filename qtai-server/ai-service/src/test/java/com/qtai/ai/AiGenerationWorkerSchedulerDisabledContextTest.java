package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.qtai.domain.ai.internal.AiGenerationWorkerScheduler;

class AiGenerationWorkerSchedulerDisabledContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiServiceApplication.class);

    @Test
    void defaultModeDoesNotRegisterGenerationWorkerScheduler() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AiGenerationWorkerScheduler.class);
        });
    }

    @Test
    void schedulerEnabledWithoutWorkerAndPersistenceDoesNotRegisterScheduler() {
        contextRunner
                .withPropertyValues("qtai.ai.worker.generation.scheduler.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AiGenerationWorkerScheduler.class);
                });
    }
}
