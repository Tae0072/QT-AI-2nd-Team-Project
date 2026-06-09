package com.qtai.domain.ai.client.deepseek;

import com.qtai.domain.ai.client.AiClientException;

public interface DeepSeekGenerationClient {

    DeepSeekGenerationResponse complete(DeepSeekGenerationRequest request) throws AiClientException;

    record DeepSeekGenerationRequest(
            String model,
            String systemPrompt,
            String userPrompt,
            Integer maxTokens,
            Double temperature
    ) {
    }

    record DeepSeekGenerationResponse(
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String model
    ) {
    }
}
