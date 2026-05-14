package com.qtai.ai.infrastructure.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek API (OpenAI 호환) SSE 스트림 클라이언트.
 *
 * <p>DECISIONS.md §6: DeepSeek API · OpenAI 호환 엔드포인트 · SSE. Anthropic SDK 금지.
 *
 * <p>WebClient 사용 이유: SseEmitter는 응답 측, DeepSeek 호출 측은 토큰 스트리밍을 Flux로 받기 위함.
 * 첫 토큰 timeout 5s / idle 30s / max 5분은 컨트롤러 측 SseEmitter timeout 옵션으로 강제.
 *
 * <p>TODO(강상민): retry-after 처리, JSON 응답 안의 [DONE] 토큰 처리,
 *               choices[0].delta.content 추출, DeepSeek 인증 헤더(K8s Secret).
 */
@Service
public class DeepSeekStreamService {

    private final WebClient webClient;
    private final String model;

    public DeepSeekStreamService(@Value("${deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
                                  @Value("${deepseek.api-key:}") String apiKey,
                                  @Value("${deepseek.model:deepseek-chat}") String model) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Flux<String> stream(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt))
        );

        return webClient.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class);
    }
}
