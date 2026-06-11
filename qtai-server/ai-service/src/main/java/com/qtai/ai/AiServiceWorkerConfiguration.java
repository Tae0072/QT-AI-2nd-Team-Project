package com.qtai.ai;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.qtai.domain.ai.internal.AiEventOutboxRepository;
import com.qtai.domain.ai.internal.AiGeneratedAssetRepository;
import com.qtai.domain.ai.internal.AiGenerationJobRepository;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor;
import com.qtai.domain.ai.internal.AiGenerationWorkerService;

@Configuration
@ConditionalOnProperty(
        name = {"qtai.ai.worker.generation.enabled", "qtai.ai.persistence.enabled"},
        havingValue = "true"
)
@EnableConfigurationProperties(AiGenerationWorkerProperties.class)
public class AiServiceWorkerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    AiGenerationWorkerService aiGenerationWorkerService(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiEventOutboxRepository eventOutboxRepository,
            AiGenerationWorkerExecutor executor,
            ObjectMapper objectMapper,
            Clock clock,
            AiGenerationWorkerProperties properties,
            @Qualifier("aiServiceTransactionManager") PlatformTransactionManager transactionManager
    ) {
        return new AiGenerationWorkerService(
                generationJobRepository,
                generatedAssetRepository,
                eventOutboxRepository,
                executor,
                objectMapper,
                clock,
                properties.batchSize(),
                transactionManager
        );
    }
}

@ConfigurationProperties(prefix = "qtai.ai.worker.generation")
class AiGenerationWorkerProperties {

    private int batchSize = 1;

    int batchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("qtai.ai.worker.generation.batch-size must be positive");
        }
        this.batchSize = batchSize;
    }
}
