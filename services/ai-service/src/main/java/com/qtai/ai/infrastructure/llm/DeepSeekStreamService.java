package com.qtai.ai.infrastructure.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * DeepSeek OpenAI-compatible API adapter.
 */
@Service
public class DeepSeekStreamService {

    private final RestClient restClient;

    @Value("${qtai.deepseek.api-key}")
    private String apiKey;

    @Value("${qtai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${qtai.deepseek.max-tokens:4096}")
    private long maxTokens;

    public DeepSeekStreamService(
        RestClient.Builder builder,
        @Value("${qtai.deepseek.base-url:https://api.deepseek.com}") String baseUrl
    ) {
        this.restClient = builder
            .baseUrl(baseUrl)
            .build();
    }

    public void stream(String systemPrompt, String userMessage, TokenConsumer onToken) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @FunctionalInterface
    public interface TokenConsumer {
        void accept(String token);
    }
}
