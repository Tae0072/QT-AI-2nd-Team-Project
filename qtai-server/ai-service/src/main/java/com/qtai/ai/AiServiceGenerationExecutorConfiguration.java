package com.qtai.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient;
import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClientHttpAdapter;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor;
import com.qtai.domain.ai.internal.DeepSeekGenerationWorkerExecutor;

@Configuration
@ConditionalOnProperty(name = "qtai.ai.worker.generation.executor.mode", havingValue = "deepseek")
@EnableConfigurationProperties(AiGenerationExecutorProperties.class)
public class AiServiceGenerationExecutorConfiguration {

    @Bean
    DeepSeekGenerationClient deepSeekGenerationClient(
            AiGenerationExecutorProperties properties,
            ObjectMapper objectMapper
    ) {
        AiGenerationExecutorProperties.DeepSeek deepSeek = properties.validatedDeepSeek();
        return new DeepSeekGenerationClientHttpAdapter(
                deepSeek.baseUrl(),
                deepSeek.apiKey(),
                deepSeek.timeoutMs(),
                objectMapper
        );
    }

    @Bean
    AiGenerationWorkerExecutor deepSeekGenerationWorkerExecutor(
            AiGenerationExecutorProperties properties,
            DeepSeekGenerationClient deepSeekGenerationClient
    ) {
        AiGenerationExecutorProperties.DeepSeek deepSeek = properties.validatedDeepSeek();
        return new DeepSeekGenerationWorkerExecutor(
                deepSeekGenerationClient,
                deepSeek.model()
        );
    }
}

@ConfigurationProperties(prefix = "qtai.ai.worker.generation.executor")
class AiGenerationExecutorProperties {

    private DeepSeek deepseek = new DeepSeek();

    public DeepSeek getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(DeepSeek deepseek) {
        this.deepseek = deepseek;
    }

    DeepSeek validatedDeepSeek() {
        if (deepseek == null) {
            throw new IllegalStateException("qtai.ai.worker.generation.executor.deepseek must be configured");
        }
        return new DeepSeek(
                requireText(deepseek.baseUrl, "qtai.ai.worker.generation.executor.deepseek.base-url"),
                requireText(deepseek.apiKey, "qtai.ai.worker.generation.executor.deepseek.api-key"),
                requireText(deepseek.model, "qtai.ai.worker.generation.executor.deepseek.model"),
                requirePositive(deepseek.timeoutMs, "qtai.ai.worker.generation.executor.deepseek.timeout-ms")
        );
    }

    private static String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
        return value;
    }

    private static int requirePositive(int value, String propertyName) {
        if (value < 1) {
            throw new IllegalStateException(propertyName + " must be positive");
        }
        return value;
    }

    static class DeepSeek {

        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
        private int timeoutMs = 3000;

        DeepSeek() {
        }

        DeepSeek(String baseUrl, String apiKey, String model, int timeoutMs) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.timeoutMs = timeoutMs;
        }

        String baseUrl() {
            return baseUrl;
        }

        String apiKey() {
            return apiKey;
        }

        String model() {
            return model;
        }

        int timeoutMs() {
            return timeoutMs;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
