package com.qtai.domain.ai.internal;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class AiGenerationWorkerScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationWorkerScheduler.class);

    private final AiGenerationWorkerService workerService;

    public AiGenerationWorkerScheduler(AiGenerationWorkerService workerService) {
        this.workerService = Objects.requireNonNull(workerService, "workerService must not be null");
    }

    @Scheduled(fixedDelayString = "${qtai.ai.worker.generation.scheduler.fixed-delay-ms:30000}")
    public void runScheduledBatch() {
        try {
            int processedCount = workerService.runBatch();
            log.debug("AI generation worker scheduler completed. processedCount={}", processedCount);
        } catch (RuntimeException exception) {
            log.warn(
                    "AI generation worker scheduler failed. handlerName=AiGenerationWorkerScheduler errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
