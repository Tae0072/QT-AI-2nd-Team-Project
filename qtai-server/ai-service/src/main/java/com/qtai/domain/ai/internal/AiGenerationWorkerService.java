package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;

public class AiGenerationWorkerService {

    private static final String EVENT_SCHEMA_VERSION = "0.1.0";
    private static final String EVENT_AGGREGATE_TYPE = "ai_generation_job";
    private static final String EVENT_STARTED = "AiGenerationJobStarted";
    private static final String EVENT_COMPLETED = "AiGenerationJobCompleted";
    private static final String EVENT_FAILED = "AiGenerationJobFailed";
    private static final String FAILURE_CODE = "GENERATION_EXECUTION_FAILED";

    private final AiGenerationJobRepository generationJobRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiEventOutboxRepository eventOutboxRepository;
    private final AiGenerationWorkerExecutor executor;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int batchSize;
    private final TransactionTemplate transactionTemplate;

    public AiGenerationWorkerService(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiEventOutboxRepository eventOutboxRepository,
            AiGenerationWorkerExecutor executor,
            ObjectMapper objectMapper,
            Clock clock,
            int batchSize,
            PlatformTransactionManager transactionManager
    ) {
        this.generationJobRepository = Objects.requireNonNull(generationJobRepository,
                "generationJobRepository must not be null");
        this.generatedAssetRepository = Objects.requireNonNull(generatedAssetRepository,
                "generatedAssetRepository must not be null");
        this.eventOutboxRepository = Objects.requireNonNull(eventOutboxRepository,
                "eventOutboxRepository must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.batchSize = batchSize;
        this.transactionTemplate = new TransactionTemplate(Objects.requireNonNull(
                transactionManager,
                "transactionManager must not be null"
        ));
    }

    public boolean runOnce() {
        ClaimedGenerationJob claimedJob = claimNextQueuedJob();
        if (claimedJob == null) {
            return false;
        }
        executeClaimedJob(claimedJob);
        return true;
    }

    public int runBatch() {
        int processed = 0;
        for (int index = 0; index < batchSize; index++) {
            if (!runOnce()) {
                break;
            }
            processed++;
        }
        return processed;
    }

    boolean runJobId(Long jobId) {
        ClaimedGenerationJob claimedJob = claimQueuedJob(jobId);
        if (claimedJob == null) {
            return false;
        }
        executeClaimedJob(claimedJob);
        return true;
    }

    private void executeClaimedJob(ClaimedGenerationJob claimedJob) {
        try {
            AiGenerationWorkerResult result = executor.execute(claimedJob.toWorkerJob());
            completeSucceededJob(claimedJob, result);
        } catch (RuntimeException exception) {
            completeFailedJob(claimedJob, exception);
        }
    }

    private ClaimedGenerationJob claimNextQueuedJob() {
        return transactionTemplate.execute(status -> generationJobRepository
                .findQueuedJobIds(AiGenerationJobStatus.QUEUED, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(this::claimQueuedJobInCurrentTransaction)
                .orElse(null));
    }

    private ClaimedGenerationJob claimQueuedJob(Long jobId) {
        return transactionTemplate.execute(status -> claimQueuedJobInCurrentTransaction(jobId));
    }

    private ClaimedGenerationJob claimQueuedJobInCurrentTransaction(Long jobId) {
        return generationJobRepository.findByIdAndStatus(jobId, AiGenerationJobStatus.QUEUED)
                .map(job -> {
                    OffsetDateTime startedAt = now();
                    job.markRunning(startedAt);
                    generationJobRepository.saveAndFlush(job);
                    eventOutboxRepository.save(startedEvent(job, startedAt));
                    return ClaimedGenerationJob.from(job, startedAt);
                })
                .orElse(null);
    }

    private void completeSucceededJob(ClaimedGenerationJob claimedJob, AiGenerationWorkerResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            AiGenerationJob job = generationJobRepository
                    .findByIdAndStatus(claimedJob.jobId(), AiGenerationJobStatus.RUNNING)
                    .orElseThrow(() -> new IllegalStateException(
                            "AI generation job is not running: " + claimedJob.jobId()
                    ));
            OffsetDateTime finishedAt = now();
            AiGeneratedAsset asset = generatedAssetRepository.save(AiGeneratedAsset.create(
                    job.getId(),
                    result.assetType(),
                    job.getTargetType(),
                    job.getTargetId(),
                    result.payloadJson(),
                    result.sourceLabel(),
                    finishedAt
            ));
            job.markSucceeded(finishedAt);
            generationJobRepository.save(job);
            eventOutboxRepository.save(completedEvent(job, asset, finishedAt));
        });
    }

    private void completeFailedJob(ClaimedGenerationJob claimedJob, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> {
            AiGenerationJob job = generationJobRepository.findById(claimedJob.jobId())
                    .orElseThrow(() -> new IllegalStateException(
                            "AI generation job is not found: " + claimedJob.jobId()
                    ));
            OffsetDateTime failedAt = now();
            job.markFailed(failureMessage(exception), failedAt);
            generationJobRepository.save(job);
            eventOutboxRepository.save(failedEvent(claimedJob, failedAt));
        });
    }

    private AiEventOutbox startedEvent(AiGenerationJob job, OffsetDateTime startedAt) {
        Map<String, Object> payload = basePayload(job.getId(), job.getJobType());
        payload.put("startedAt", format(startedAt));
        return outbox(EVENT_STARTED, job.getId(), payload, startedAt);
    }

    private AiEventOutbox completedEvent(
            AiGenerationJob job,
            AiGeneratedAsset asset,
            OffsetDateTime finishedAt
    ) {
        Map<String, Object> payload = basePayload(job.getId(), job.getJobType());
        payload.put("assetId", asset.getId());
        payload.put("finishedAt", format(finishedAt));
        return outbox(EVENT_COMPLETED, job.getId(), payload, finishedAt);
    }

    private AiEventOutbox failedEvent(ClaimedGenerationJob claimedJob, OffsetDateTime failedAt) {
        Map<String, Object> payload = basePayload(claimedJob.jobId(), claimedJob.jobType());
        payload.put("failureCode", FAILURE_CODE);
        payload.put("failedAt", format(failedAt));
        return outbox(EVENT_FAILED, claimedJob.jobId(), payload, failedAt);
    }

    private AiEventOutbox outbox(
            String eventName,
            Long jobId,
            Map<String, Object> payload,
            OffsetDateTime createdAt
    ) {
        return AiEventOutbox.create(
                UUID.randomUUID().toString(),
                eventName,
                EVENT_AGGREGATE_TYPE,
                "job-" + jobId,
                EVENT_SCHEMA_VERSION,
                toJson(payload),
                null,
                createdAt
        );
    }

    private Map<String, Object> basePayload(Long jobId, AiGenerationJobType jobType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", jobId);
        payload.put("resultType", jobType.name());
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI generation worker event payload serialization failed", exception);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static String format(OffsetDateTime value) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
    }

    private static String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }

    private record ClaimedGenerationJob(
            Long jobId,
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            OffsetDateTime startedAt
    ) {

        private static ClaimedGenerationJob from(AiGenerationJob job, OffsetDateTime startedAt) {
            return new ClaimedGenerationJob(
                    job.getId(),
                    job.getJobType(),
                    job.getTargetType(),
                    job.getTargetId(),
                    job.getPromptVersionId(),
                    startedAt
            );
        }

        private AiGenerationWorkerJob toWorkerJob() {
            return new AiGenerationWorkerJob(
                    jobId,
                    jobType,
                    targetType,
                    targetId,
                    promptVersionId,
                    startedAt
            );
        }
    }
}
