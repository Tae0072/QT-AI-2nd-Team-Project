package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;

import com.qtai.ai.AiServiceApplication;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;

class AiGenerationRealExecutorRuntimeToggleTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiServiceApplication.class)
            .withPropertyValues("spring.profiles.active=test");

    @Test
    void defaultModeUsesTestFakeExecutorAndDoesNotRegisterRealSkeleton() {
        contextRunner
                .withUserConfiguration(FakeExecutorConfig.class)
                .withPropertyValues(workerPersistenceProperties("runtime_toggle_default"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FakeGenerationWorkerExecutor.class);
                    assertThat(context).doesNotHaveBean(DeepSeekGenerationWorkerExecutor.class);
                    assertThat(context.getBean(AiGenerationWorkerExecutor.class))
                            .isInstanceOf(FakeGenerationWorkerExecutor.class);

                    AiGenerationJob job = saveQueuedJob(context, 35L);

                    assertThat(context.getBean(AiGenerationWorkerService.class).runOnce()).isTrue();

                    assertSucceededWithFakeResult(context, job.getId(), 35L);
                });
    }

    @Test
    void noneModeUsesTestFakeExecutorAndDoesNotRegisterRealSkeleton() {
        contextRunner
                .withUserConfiguration(FakeExecutorConfig.class)
                .withPropertyValues("qtai.ai.worker.generation.executor.mode=none")
                .withPropertyValues(workerPersistenceProperties("runtime_toggle_none"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FakeGenerationWorkerExecutor.class);
                    assertThat(context).doesNotHaveBean(DeepSeekGenerationWorkerExecutor.class);

                    AiGenerationJob job = saveQueuedJob(context, 36L);

                    assertThat(context.getBean(AiGenerationWorkerService.class).runOnce()).isTrue();

                    assertSucceededWithFakeResult(context, job.getId(), 36L);
                });
    }

    @Test
    void deepSeekModeUsesRealSkeletonExecutorWithoutFakeExecutor() {
        contextRunner
                .withPropertyValues(deepSeekModeProperties())
                .withPropertyValues(workerPersistenceProperties("runtime_toggle_deepseek"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DeepSeekGenerationWorkerExecutor.class);
                    assertThat(context).doesNotHaveBean(FakeGenerationWorkerExecutor.class);
                    assertThat(context.getBean(AiGenerationWorkerExecutor.class))
                            .isInstanceOf(DeepSeekGenerationWorkerExecutor.class);

                    AiGenerationJob job = saveQueuedJob(context, 37L);

                    assertThat(context.getBean(AiGenerationWorkerService.class).runOnce()).isTrue();

                    assertThat(context.getBean(AiGenerationJobRepository.class).findById(job.getId()))
                            .get()
                            .satisfies(failedJob -> {
                                assertThat(failedJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
                                assertThat(failedJob.getErrorMessage())
                                        .contains("DeepSeek generation executor skeleton is not implemented");
                            });
                    assertThat(context.getBean(AiGeneratedAssetRepository.class).findAll()).isEmpty();
                    assertThat(context.getBean(AiEventOutboxRepository.class)
                            .findAll(Sort.by("createdAt").and(Sort.by("id"))))
                            .extracting(AiEventOutbox::getEventName)
                            .containsExactly("AiGenerationJobStarted", "AiGenerationJobFailed");
                });
    }

    @Test
    void deepSeekModeKeepsFailFastWhenRequiredPropertiesAreMissing() {
        contextRunner
                .withPropertyValues("qtai.ai.worker.generation.executor.mode=deepseek")
                .withPropertyValues("qtai.ai.worker.generation.executor.deepseek.base-url=http://localhost:65535")
                .withPropertyValues("qtai.ai.worker.generation.executor.deepseek.api-key=redacted-generation-executor-value")
                .withPropertyValues(workerPersistenceProperties("runtime_toggle_fail_fast"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("qtai.ai.worker.generation.executor.deepseek.model");
                });
    }

    private static AiGenerationJob saveQueuedJob(ApplicationContext context, Long targetId) {
        return context.getBean(AiGenerationJobRepository.class).saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                targetId,
                1L,
                BASE_TIME.plusMinutes(targetId)
        ));
    }

    private static void assertSucceededWithFakeResult(ApplicationContext context, Long jobId, Long targetId) {
        assertThat(context.getBean(AiGenerationJobRepository.class).findById(jobId))
                .map(AiGenerationJob::getStatus)
                .contains(AiGenerationJobStatus.SUCCEEDED);
        assertThat(context.getBean(AiGeneratedAssetRepository.class).findAll())
                .singleElement()
                .satisfies(asset -> {
                    assertThat(asset.getGenerationJobId()).isEqualTo(jobId);
                    assertThat(asset.getAssetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
                    assertThat(asset.getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
                    assertThat(asset.getTargetId()).isEqualTo(targetId);
                    assertThat(asset.getPayloadJson()).contains("Allowed runtime toggle summary");
                    assertThat(asset.getSourceLabel()).isEqualTo("AI-WORKER-TOGGLE-FAKE");
                });
        assertThat(context.getBean(AiEventOutboxRepository.class)
                .findAll(Sort.by("createdAt").and(Sort.by("id"))))
                .extracting(AiEventOutbox::getEventName)
                .containsExactly("AiGenerationJobStarted", "AiGenerationJobCompleted");
    }

    private static String[] workerPersistenceProperties(String databaseName) {
        return new String[] {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.worker.generation.enabled=true",
                "qtai.ai.worker.generation.scheduler.enabled=false",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_generation_executor_" + databaseName
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        };
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

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    @TestConfiguration
    static class FakeExecutorConfig {

        @Bean
        FakeGenerationWorkerExecutor fakeGenerationWorkerExecutor() {
            return new FakeGenerationWorkerExecutor();
        }
    }

    static class FakeGenerationWorkerExecutor implements AiGenerationWorkerExecutor {

        @Override
        public AiGenerationWorkerResult execute(AiGenerationWorkerJob job) {
            return AiGenerationWorkerResult.of(
                    AiGeneratedAssetType.EXPLANATION,
                    "{\"summary\":\"Allowed runtime toggle summary\"}",
                    "AI-WORKER-TOGGLE-FAKE"
            );
        }
    }
}
