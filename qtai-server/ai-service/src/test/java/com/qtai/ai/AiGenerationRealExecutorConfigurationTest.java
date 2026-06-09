package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor;
import com.qtai.domain.ai.internal.AiGenerationWorkerService;
import com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutor;

class AiGenerationRealExecutorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiServiceApplication.class)
            .withPropertyValues("spring.profiles.active=test");

    @Test
    void defaultModeDoesNotRegisterProductionExecutor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AiGenerationWorkerExecutor.class);
        });
    }

    @Test
    void deepSeekModeRegistersSkeletonExecutorWhenRequiredPropertiesExist() {
        contextRunner
                .withPropertyValues(deepSeekModeProperties())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AiGenerationWorkerExecutor.class);
                    assertThat(context.getBean(AiGenerationWorkerExecutor.class))
                            .isInstanceOf(DeepSeekGenerationWorkerExecutor.class);
                });
    }

    @Test
    void deepSeekModeFailsFastWhenBaseUrlIsMissing() {
        contextRunner
                .withPropertyValues(deepSeekModePropertiesExcept(
                        "qtai.ai.worker.generation.executor.deepseek.base-url"
                ))
                .run(context -> assertFailedWith(
                        context.getStartupFailure(),
                        "qtai.ai.worker.generation.executor.deepseek.base-url"
                ));
    }

    @Test
    void deepSeekModeFailsFastWhenApiKeyIsMissing() {
        contextRunner
                .withPropertyValues(deepSeekModePropertiesExcept(
                        "qtai.ai.worker.generation.executor.deepseek.api-key"
                ))
                .run(context -> assertFailedWith(
                        context.getStartupFailure(),
                        "qtai.ai.worker.generation.executor.deepseek.api-key"
                ));
    }

    @Test
    void deepSeekModeFailsFastWhenModelIsMissing() {
        contextRunner
                .withPropertyValues(deepSeekModePropertiesExcept(
                        "qtai.ai.worker.generation.executor.deepseek.model"
                ))
                .run(context -> assertFailedWith(
                        context.getStartupFailure(),
                        "qtai.ai.worker.generation.executor.deepseek.model"
                ));
    }

    @Test
    void deepSeekModeFailsFastWhenTimeoutIsNotPositive() {
        contextRunner
                .withPropertyValues(deepSeekModePropertiesExcept(
                        "qtai.ai.worker.generation.executor.deepseek.timeout-ms"
                ))
                .withPropertyValues("qtai.ai.worker.generation.executor.deepseek.timeout-ms=0")
                .run(context -> assertFailedWith(
                        context.getStartupFailure(),
                        "qtai.ai.worker.generation.executor.deepseek.timeout-ms"
                ));
    }

    @Test
    void workerEnabledModeCanUseSkeletonExecutorBean() {
        contextRunner
                .withPropertyValues(deepSeekModeProperties())
                .withPropertyValues(workerPersistenceProperties())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DeepSeekGenerationWorkerExecutor.class);
                    assertThat(context).hasSingleBean(AiGenerationWorkerService.class);
                });
    }

    private static String[] deepSeekModeProperties() {
        return new String[] {
                "qtai.ai.worker.generation.executor.mode=deepseek",
                "qtai.ai.worker.generation.executor.deepseek.base-url=http://localhost:65535",
                "qtai.ai.worker.generation.executor.deepseek.api-key=redacted-generation-executor-value",
                "qtai.ai.worker.generation.executor.deepseek.model=deepseek-test-model",
                "qtai.ai.worker.generation.executor.deepseek.timeout-ms=3000"
        };
    }

    private static String[] deepSeekModePropertiesExcept(String excludedPropertyName) {
        return Arrays.stream(deepSeekModeProperties())
                .filter(property -> !property.startsWith(excludedPropertyName + "="))
                .toArray(String[]::new);
    }

    private static String[] workerPersistenceProperties() {
        return new String[] {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.worker.generation.enabled=true",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_generation_real_executor_context;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        };
    }

    private static void assertFailedWith(Throwable startupFailure, String expectedMessage) {
        assertThat(startupFailure).isNotNull();
        assertThat(rootCause(startupFailure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
