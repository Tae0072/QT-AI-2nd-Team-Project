package com.qtai.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.qtai.domain.ai.internal.AiGenerationWorkerScheduler;
import com.qtai.domain.ai.internal.AiGenerationWorkerService;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = {
                "qtai.ai.persistence.enabled",
                "qtai.ai.worker.generation.enabled",
                "qtai.ai.worker.generation.scheduler.enabled"
        },
        havingValue = "true"
)
public class AiServiceWorkerSchedulerConfiguration {

    @Bean
    AiGenerationWorkerScheduler aiGenerationWorkerScheduler(AiGenerationWorkerService workerService) {
        return new AiGenerationWorkerScheduler(workerService);
    }
}
