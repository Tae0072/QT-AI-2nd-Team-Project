package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.qtai.domain.ai.internal.AiGeneratedAssetType;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;
import com.qtai.domain.ai.internal.AiGenerationWorkerService;

class AiGenerationWorkerEnabledContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiServiceApplication.class)
            .withPropertyValues(workerProperties());

    @Test
    void workerEnabledModeRegistersGenerationWorkerWhenExecutorExists() {
        contextRunner
                .withBean(AiGenerationWorkerExecutor.class, () -> job -> AiGenerationWorkerResult.of(
                        AiGeneratedAssetType.EXPLANATION,
                        "{\"summary\":\"Allowed worker summary\"}",
                        "AI-WORKER"
                ))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AiGenerationWorkerService.class);
                });
    }

    @Test
    void workerEnabledModeFailsFastWhenExecutorIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context.getStartupFailure()))
                    .hasMessageContaining("AiGenerationWorkerExecutor");
        });
    }

    private static String[] workerProperties() {
        return new String[] {
                "spring.profiles.active=test",
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.worker.generation.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_generation_worker_enabled_context;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
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
