package com.qtai.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Anthropic Claude API 호출 서비스.
 * SSE 스트리밍으로 token 단위 응답을 콜백으로 전달합니다.
 *
 * 사용 예 (Anthropic Java SDK):
 * <pre>
 *   MessageCreateParams params = MessageCreateParams.builder()
 *       .maxTokens(maxTokens)
 *       .model(Model.CLAUDE_SONNET_4_5)
 *       .system(systemPrompt)
 *       .addUserMessage(userMessage)
 *       .build();
 *
 *   try (StreamResponse&lt;RawMessageStreamEvent&gt; stream =
 *           client.messages().createStreaming(params)) {
 *       stream.stream().forEach(event -&gt; {
 *           // event 구조에 따라 text delta를 추출해 onToken 호출
 *       });
 *   }
 * </pre>
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

    /**
     * Claude에 메시지를 보내고 SSE 스트리밍 응답을 수신해 onToken 콜백을 호출.
     */
    public void stream(String systemPrompt, String userMessage, TokenConsumer onToken) {
        // TODO: MessageCreateParams.builder() 구성 + client.messages().createStreaming() 호출
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @FunctionalInterface
    public interface TokenConsumer {
        void accept(String token);
    }
}
