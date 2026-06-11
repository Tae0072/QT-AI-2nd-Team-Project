package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.qtai.domain.ai.internal.AiGeneratedAssetType;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;
import com.qtai.domain.ai.internal.AiGenerationWorkerScheduler;
import com.qtai.domain.ai.internal.AiGenerationWorkerService;

class AiGenerationWorkerSchedulerEnabledContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiServiceApplication.class)
            .withPropertyValues(workerSchedulerProperties());

    @Test
    void schedulerEnabledModeRegistersGenerationWorkerSchedulerWhenExecutorExists() {
        contextRunner
                .withBean(AiGenerationWorkerExecutor.class, () -> job -> AiGenerationWorkerResult.of(
                        AiGeneratedAssetType.EXPLANATION,
                        "{\"summary\":\"Allowed scheduler summary\"}",
                        "AI-WORKER"
                ))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AiGenerationWorkerService.class);
                    assertThat(context).hasSingleBean(AiGenerationWorkerScheduler.class);
                });
    }

    @Test
    void schedulerEnabledModeFailsFastWhenExecutorIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context.getStartupFailure()))
                    .hasMessageContaining("AiGenerationWorkerExecutor");
        });
    }

    private static String[] workerSchedulerProperties() {
        return new String[] {
                "spring.profiles.active=test",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.worker.generation.enabled=true",
                "qtai.ai.worker.generation.scheduler.enabled=true",
                "qtai.ai.worker.generation.scheduler.fixed-delay-ms=600000",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_generation_worker_scheduler_enabled_context;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        };
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
