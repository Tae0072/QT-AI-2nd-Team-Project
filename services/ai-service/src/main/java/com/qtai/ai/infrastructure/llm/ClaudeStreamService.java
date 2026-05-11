package com.qtai.ai.infrastructure.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Anthropic Claude API adapter.
 */
@Service
public class ClaudeStreamService {

    @Value("${qtai.anthropic.api-key}")
    private String apiKey;

    @Value("${qtai.anthropic.model:claude-sonnet-4-5}")
    private String model;

    @Value("${qtai.anthropic.max-tokens:4096}")
    private long maxTokens;

    private AnthropicClient client;

    @PostConstruct
    void init() {
        this.client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
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
