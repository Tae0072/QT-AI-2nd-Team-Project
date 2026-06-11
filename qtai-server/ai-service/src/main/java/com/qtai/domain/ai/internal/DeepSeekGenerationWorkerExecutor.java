package com.qtai.domain.ai.internal;

import java.util.Objects;

public final class DeepSeekGenerationWorkerExecutor implements AiGenerationWorkerExecutor {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutMs;

    public DeepSeekGenerationWorkerExecutor(String baseUrl, String apiKey, String model, int timeoutMs) {
        this.baseUrl = requireText(baseUrl, "qtai.ai.worker.generation.executor.deepseek.base-url");
        this.apiKey = requireText(apiKey, "qtai.ai.worker.generation.executor.deepseek.api-key");
        this.model = requireText(model, "qtai.ai.worker.generation.executor.deepseek.model");
        this.timeoutMs = requirePositive(timeoutMs, "qtai.ai.worker.generation.executor.deepseek.timeout-ms");
    }

    @Override
    public AiGenerationWorkerResult execute(AiGenerationWorkerJob job) {
        Objects.requireNonNull(job, "job must not be null");
        throw new UnsupportedOperationException("DeepSeek generation executor skeleton is not implemented");
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
}
