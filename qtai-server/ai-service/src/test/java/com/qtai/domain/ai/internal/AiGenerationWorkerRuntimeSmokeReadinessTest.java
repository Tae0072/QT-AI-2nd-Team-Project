package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;

@SpringBootTest(
        classes = {AiServiceApplication.class, AiGenerationWorkerRuntimeSmokeReadinessTest.FakeExecutorConfig.class},
        properties = {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.worker.generation.enabled=true",
                "qtai.ai.worker.generation.scheduler.enabled=false",
                "qtai.ai.worker.generation.batch-size=2",
                "qtai.ai.client.mode=mock",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_generation_worker_runtime_smoke;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiGenerationWorkerRuntimeSmokeReadinessTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private ApplicationContext context;
    @Autowired
    private AiGenerationWorkerService workerService;
    @Autowired
    private AiGenerationJobRepository generationJobRepository;
    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;
    @Autowired
    private AiEventOutboxRepository eventOutboxRepository;
    @Autowired
    private FakeRuntimeSmokeGenerationWorkerExecutor executor;

    @BeforeEach
    void setUp() {
        eventOutboxRepository.deleteAll();
        generatedAssetRepository.deleteAll();
        generationJobRepository.deleteAll();
        executor.reset();
    }

    @Test
    void runtimeSmokeRegistersWorkerAndKeepsSchedulerDisabled() {
        assertThat(context.getBeansOfType(AiGenerationWorkerService.class)).hasSize(1);
        assertThat(context.getBeansOfType(FakeRuntimeSmokeGenerationWorkerExecutor.class)).hasSize(1);
        assertThat(context.getBeansOfType(AiGenerationWorkerScheduler.class)).isEmpty();
    }

    @Test
    void runtimeSmokeRunsQueuedJobAndPersistsAssetAndOutboxEvents() {
        AiGenerationJob job = saveQueuedJob(35L, BASE_TIME);

        assertThat(workerService.runOnce()).isTrue();

        assertThat(executor.executedJobs())
                .singleElement()
                .satisfies(executedJob -> {
                    assertThat(executedJob.jobId()).isEqualTo(job.getId());
                    assertThat(executedJob.jobType()).isEqualTo(AiGenerationJobType.EXPLANATION);
                    assertThat(executedJob.targetType()).isEqualTo(AiTargetType.QT_PASSAGE);
                    assertThat(executedJob.targetId()).isEqualTo(35L);
                    assertThat(executedJob.promptVersionId()).isEqualTo(1L);
                    assertThat(executedJob.startedAt()).isNotNull();
                });
        assertThat(generationJobRepository.findById(job.getId()))
                .map(AiGenerationJob::getStatus)
                .contains(AiGenerationJobStatus.SUCCEEDED);
        assertThat(generatedAssetRepository.findAll())
                .singleElement()
                .satisfies(asset -> {
                    assertThat(asset.getGenerationJobId()).isEqualTo(job.getId());
                    assertThat(asset.getAssetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
                    assertThat(asset.getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
                    assertThat(asset.getTargetId()).isEqualTo(35L);
                    assertThat(asset.getPayloadJson()).contains("Allowed runtime smoke summary");
                    assertThat(asset.getSourceLabel()).isEqualTo("AI-WORKER-SMOKE");
                });

        assertThat(eventOutboxRepository.findAll(Sort.by("createdAt").and(Sort.by("id"))))
                .extracting(AiEventOutbox::getEventName)
                .containsExactly("AiGenerationJobStarted", "AiGenerationJobCompleted");
    }

    @Test
    void runtimeSmokeRunBatchStopsAtConfiguredBatchSize() {
        saveQueuedJob(36L, BASE_TIME);
        saveQueuedJob(37L, BASE_TIME.plusMinutes(1));
        saveQueuedJob(38L, BASE_TIME.plusMinutes(2));

        assertThat(workerService.runBatch()).isEqualTo(2);

        assertThat(executor.executedJobs()).hasSize(2);
        assertThat(generatedAssetRepository.findAll()).hasSize(2);
        assertThat(generationJobRepository.findAll())
                .extracting(AiGenerationJob::getStatus)
                .containsExactlyInAnyOrder(
                        AiGenerationJobStatus.SUCCEEDED,
                        AiGenerationJobStatus.SUCCEEDED,
                        AiGenerationJobStatus.QUEUED
                );
    }

    @Test
    void runtimeSmokeRecordsFailedOutboxWhenFakeExecutorFails() {
        AiGenerationJob job = saveQueuedJob(39L, BASE_TIME);
        executor.failWith(new IllegalStateException("runtime smoke executor failure"));

        assertThat(workerService.runOnce()).isTrue();

        assertThat(generationJobRepository.findById(job.getId()))
                .get()
                .satisfies(failedJob -> {
                    assertThat(failedJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
                    assertThat(failedJob.getErrorMessage()).contains("runtime smoke executor failure");
                });
        assertThat(generatedAssetRepository.findAll()).isEmpty();
        assertThat(eventOutboxRepository.findAll(Sort.by("createdAt").and(Sort.by("id"))))
                .extracting(AiEventOutbox::getEventName)
                .containsExactly("AiGenerationJobStarted", "AiGenerationJobFailed");
    }

    private AiGenerationJob saveQueuedJob(Long targetId, OffsetDateTime createdAt) {
        return generationJobRepository.saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                targetId,
                1L,
                createdAt
        ));
    }

    @TestConfiguration
    static class FakeExecutorConfig {

        @Bean
        FakeRuntimeSmokeGenerationWorkerExecutor aiGenerationWorkerExecutor() {
            return new FakeRuntimeSmokeGenerationWorkerExecutor();
        }
    }

    static class FakeRuntimeSmokeGenerationWorkerExecutor implements AiGenerationWorkerExecutor {

        private final List<AiGenerationWorkerJob> executedJobs = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public AiGenerationWorkerResult execute(AiGenerationWorkerJob job) {
            executedJobs.add(job);
            if (failure != null) {
                throw failure;
            }
            return AiGenerationWorkerResult.of(
                    AiGeneratedAssetType.EXPLANATION,
                    "{\"summary\":\"Allowed runtime smoke summary\"}",
                    "AI-WORKER-SMOKE"
            );
        }

        private List<AiGenerationWorkerJob> executedJobs() {
            return executedJobs;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        private void reset() {
            executedJobs.clear();
            failure = null;
        }
    }
}
