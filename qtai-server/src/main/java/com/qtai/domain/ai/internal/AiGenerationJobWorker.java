package com.qtai.domain.ai.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AiGenerationJobWorker {

    private final AiGenerationJobRunner runner;
    private final boolean enabled;
    private final int batchSize;

    AiGenerationJobWorker(
            AiGenerationJobRunner runner,
            @Value("${ai.generation.worker.enabled:true}") boolean enabled,
            @Value("${ai.generation.worker.batch-size:5}") int batchSize
    ) {
        this.runner = runner;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ai.generation.worker.fixed-delay-ms:10000}")
    void pollQueuedJobs() {
        if (!enabled) {
            return;
        }
        try {
            runner.runQueuedBatch(batchSize);
        } catch (RuntimeException exception) {
            log.warn("AI generation worker polling failed. errorType={}", exception.getClass().getSimpleName());
        }
    }
}
