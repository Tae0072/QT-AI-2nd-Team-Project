package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AiGenerationJobWorker {

    private final AiGenerationJobRunner runner;
    private final AiBatchMonitoringService monitoringService;
    private final boolean enabled;
    private final int batchSize;
    private final long runningTimeoutMs;
    private final Clock clock;

    AiGenerationJobWorker(
            AiGenerationJobRunner runner,
            AiBatchMonitoringService monitoringService,
            @Value("${ai.generation.worker.enabled:true}") boolean enabled,
            @Value("${ai.generation.worker.batch-size:5}") int batchSize,
            @Value("${ai.generation.worker.running-timeout-ms:300000}") long runningTimeoutMs,
            Clock clock
    ) {
        this.runner = runner;
        this.monitoringService = monitoringService;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.runningTimeoutMs = runningTimeoutMs;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${ai.generation.worker.fixed-delay-ms:10000}")
    void pollQueuedJobs() {
        if (!enabled) {
            return;
        }
        // 폴링마다 먼저 고착 RUNNING job을 회수(P1-3). 회수 실패는 본 폴링을 막지 않는다.
        try {
            int sweptCount = runner.sweepStaleRunningJobs(runningTimeoutMs, batchSize);
            if (sweptCount > 0) {
                log.warn("AI generation worker swept stale RUNNING jobs. sweptCount={}", sweptCount);
            }
        } catch (RuntimeException exception) {
            log.warn("AI generation worker stale-sweep failed. errorType={}, errorMessage={}",
                    exception.getClass().getSimpleName(), exception.getMessage());
        }
        OffsetDateTime startedAt = now();
        try {
            int processedCount = runner.runQueuedBatch(batchSize);
            if (processedCount > 0) {
                log.info("AI generation worker processed jobs. processedCount={}", processedCount);
            }
        } catch (RuntimeException exception) {
            OffsetDateTime finishedAt = now();
            recordBatchRun(failedCommand(exception, startedAt, finishedAt));
            log.warn(
                    "AI generation worker polling failed. errorType={}, errorMessage={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    private void recordBatchRun(AiBatchRunLogCommand command) {
        try {
            monitoringService.record(command);
        } catch (RuntimeException exception) {
            log.warn(
                    "AI batch monitoring write failed. batchName={}, errorType={}, errorMessage={}",
                    command.batchName(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    private static AiBatchRunLogCommand failedCommand(
            RuntimeException exception,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt
    ) {
        return new AiBatchRunLogCommand(
                AiBatchName.AI_GENERATION_WORKER_POLL,
                AiBatchRunStatus.FAILED,
                0,
                0,
                0,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                startedAt,
                finishedAt
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
