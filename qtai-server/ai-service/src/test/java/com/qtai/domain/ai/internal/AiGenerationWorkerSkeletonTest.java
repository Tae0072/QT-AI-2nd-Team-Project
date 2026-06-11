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
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.ai.AiServiceApplication;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;

@SpringBootTest(
        classes = {AiServiceApplication.class, AiGenerationWorkerSkeletonTest.FakeExecutorConfig.class},
        properties = {
                "qtai.ai.persistence.enabled=true",
                "qtai.ai.worker.generation.enabled=true",
                "qtai.ai.worker.generation.batch-size=2",
                "qtai.ai.persistence.url=jdbc:h2:mem:ai_generation_worker_skeleton;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "qtai.ai.persistence.username=sa",
                "qtai.ai.persistence.driver-class-name=org.h2.Driver",
                "qtai.ai.persistence.ddl-auto=create-drop",
                "qtai.ai.persistence.flyway-enabled=false"
        }
)
@ActiveProfiles("test")
class AiGenerationWorkerSkeletonTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Autowired
    private AiGenerationWorkerService workerService;
    @Autowired
    private AiGenerationJobRepository generationJobRepository;
    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;
    @Autowired
    private AiEventOutboxRepository eventOutboxRepository;
    @Autowired
    private FakeGenerationWorkerExecutor executor;

    @BeforeEach
    void setUp() {
        eventOutboxRepository.deleteAll();
        generatedAssetRepository.deleteAll();
        generationJobRepository.deleteAll();
        executor.reset();
    }

    @Test
    void runOnceReturnsFalseWhenQueuedJobDoesNotExist() {
        assertThat(workerService.runOnce()).isFalse();

        assertThat(executor.executedJobs()).isEmpty();
        assertThat(eventOutboxRepository.findAll()).isEmpty();
    }

    @Test
    void runOnceMarksJobSucceededAndAppendsStartedAndCompletedOutboxEvents() {
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
                    assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
                    assertThat(asset.getPayloadJson()).contains("Allowed worker summary");
                    assertThat(asset.getSourceLabel()).isEqualTo("AI-WORKER");
                });

        List<AiEventOutbox> events = eventOutboxRepository.findAll(Sort.by("createdAt").and(Sort.by("id")));
        assertThat(events).extracting(AiEventOutbox::getEventName)
                .containsExactly("AiGenerationJobStarted", "AiGenerationJobCompleted");
        assertThat(events.get(0).getPayloadJson())
                .contains("\"jobId\":" + job.getId())
                .contains("\"resultType\":\"EXPLANATION\"")
                .contains("\"startedAt\"");
        assertThat(events.get(1).getPayloadJson())
                .contains("\"jobId\":" + job.getId())
                .contains("\"assetId\"")
                .contains("\"finishedAt\"");
    }

    @Test
    void runOnceMarksJobFailedAndAppendsFailedOutboxEventWithoutPropagatingExecutorException() {
        AiGenerationJob job = saveQueuedJob(36L, BASE_TIME);
        executor.failWith(new IllegalStateException("generation executor failed"));

        assertThat(workerService.runOnce()).isTrue();

        assertThat(generationJobRepository.findById(job.getId()))
                .get()
                .satisfies(failedJob -> {
                    assertThat(failedJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
                    assertThat(failedJob.getErrorMessage()).contains("generation executor failed");
                });
        assertThat(generatedAssetRepository.findAll()).isEmpty();

        List<AiEventOutbox> events = eventOutboxRepository.findAll(Sort.by("createdAt").and(Sort.by("id")));
        assertThat(events).extracting(AiEventOutbox::getEventName)
                .containsExactly("AiGenerationJobStarted", "AiGenerationJobFailed");
        assertThat(events.get(1).getPayloadJson())
                .contains("\"jobId\":" + job.getId())
                .contains("\"failureCode\":\"GENERATION_EXECUTION_FAILED\"")
                .contains("\"failedAt\"");
    }

    @Test
    void runOnceMarksJobFailedWhenExecutorResultViolatesContract() {
        AiGenerationJob job = saveQueuedJob(37L, BASE_TIME);
        executor.returnPayload("[{\"summary\":\"Allowed worker summary\"}]", "AI-WORKER");

        assertThat(workerService.runOnce()).isTrue();

        assertThat(generationJobRepository.findById(job.getId()))
                .get()
                .satisfies(failedJob -> {
                    assertThat(failedJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
                    assertThat(failedJob.getErrorMessage()).contains("payloadJson must be a JSON object");
                });
        assertThat(generatedAssetRepository.findAll()).isEmpty();

        List<AiEventOutbox> events = eventOutboxRepository.findAll(Sort.by("createdAt").and(Sort.by("id")));
        assertThat(events).extracting(AiEventOutbox::getEventName)
                .containsExactly("AiGenerationJobStarted", "AiGenerationJobFailed");
        assertThat(events.get(1).getPayloadJson())
                .contains("\"jobId\":" + job.getId())
                .contains("\"failureCode\":\"GENERATION_EXECUTION_FAILED\"");
    }

    @Test
    void runJobIdSkipsWhenJobIsAlreadyClaimedByAnotherWorker() {
        AiGenerationJob job = saveQueuedJob(38L, BASE_TIME);
        job.markRunning(BASE_TIME.plusMinutes(1));
        generationJobRepository.saveAndFlush(job);

        assertThat(workerService.runJobId(job.getId())).isFalse();

        assertThat(executor.executedJobs()).isEmpty();
        assertThat(eventOutboxRepository.findAll()).isEmpty();
        assertThat(generationJobRepository.findById(job.getId()))
                .map(AiGenerationJob::getStatus)
                .contains(AiGenerationJobStatus.RUNNING);
    }

    @Test
    void runBatchStopsAtConfiguredBatchSize() {
        saveQueuedJob(39L, BASE_TIME.plusMinutes(1));
        saveQueuedJob(40L, BASE_TIME.plusMinutes(2));
        saveQueuedJob(41L, BASE_TIME.plusMinutes(3));

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
        FakeGenerationWorkerExecutor aiGenerationWorkerExecutor() {
            return new FakeGenerationWorkerExecutor();
        }
    }

    static class FakeGenerationWorkerExecutor implements AiGenerationWorkerExecutor {

        private final List<AiGenerationWorkerJob> executedJobs = new ArrayList<>();
        private RuntimeException failure;
        private String payloadJson = "{\"summary\":\"Allowed worker summary\"}";
        private String sourceLabel = "AI-WORKER";

        @Override
        public AiGenerationWorkerResult execute(AiGenerationWorkerJob job) {
            executedJobs.add(job);
            if (failure != null) {
                throw failure;
            }
            return AiGenerationWorkerResult.of(
                    AiGeneratedAssetType.EXPLANATION,
                    payloadJson,
                    sourceLabel
            );
        }

        private List<AiGenerationWorkerJob> executedJobs() {
            return executedJobs;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        private void returnPayload(String payloadJson, String sourceLabel) {
            this.payloadJson = payloadJson;
            this.sourceLabel = sourceLabel;
        }

        private void reset() {
            executedJobs.clear();
            failure = null;
            payloadJson = "{\"summary\":\"Allowed worker summary\"}";
            sourceLabel = "AI-WORKER";
        }
    }
}
