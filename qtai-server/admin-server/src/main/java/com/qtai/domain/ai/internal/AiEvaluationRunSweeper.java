package com.qtai.domain.ai.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
class AiEvaluationRunSweeper {

    private final AiEvaluationRunService evaluationRunService;
    private final boolean enabled;
    private final long runningTimeoutMs;
    private final int batchSize;

    AiEvaluationRunSweeper(
            AiEvaluationRunService evaluationRunService,
            @Value("${ai.evaluation-run.sweeper.enabled:true}") boolean enabled,
            @Value("${ai.evaluation-run.sweeper.running-timeout-ms:300000}") long runningTimeoutMs,
            @Value("${ai.evaluation-run.sweeper.batch-size:20}") int batchSize
    ) {
        this.evaluationRunService = evaluationRunService;
        this.enabled = enabled;
        this.runningTimeoutMs = runningTimeoutMs;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ai.evaluation-run.sweeper.fixed-delay-ms:60000}")
    void sweepStaleRunningRuns() {
        if (!enabled) {
            return;
        }
        try {
            int sweptCount = evaluationRunService.sweepStaleRunningRuns(runningTimeoutMs, batchSize);
            if (sweptCount > 0) {
                log.warn("AI evaluation run sweeper marked stale RUNNING runs failed. sweptCount={}", sweptCount);
            }
        } catch (RuntimeException exception) {
            log.warn("AI evaluation run sweeper failed. errorType={}, errorMessage={}",
                    exception.getClass().getSimpleName(), exception.getMessage());
        }
    }
}
