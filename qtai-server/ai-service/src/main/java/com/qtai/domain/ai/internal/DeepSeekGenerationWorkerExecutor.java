package com.qtai.domain.ai.internal;

import java.util.Objects;

import com.qtai.domain.ai.client.deepseek.DeepSeekGenerationClient;

public final class DeepSeekGenerationWorkerExecutor implements AiGenerationWorkerExecutor {

    private final DeepSeekGenerationClient client;
    private final String model;

    public DeepSeekGenerationWorkerExecutor(DeepSeekGenerationClient client, String model) {
        this.client = Objects.requireNonNull(client, "deepSeekGenerationClient must not be null");
        this.model = requireText(model, "qtai.ai.worker.generation.executor.deepseek.model");
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

    DeepSeekGenerationClient client() {
        return client;
    }

    String model() {
        return model;
    }
}
