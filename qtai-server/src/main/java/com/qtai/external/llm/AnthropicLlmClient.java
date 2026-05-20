package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;
import org.springframework.stereotype.Component;

/**
 * LLM 호출 구현체.
 *
 * v3.1 게이트: qtai-server에서 Anthropic SDK 직접 사용 금지 — DeepSeek API만 허용.
 * 클래스명은 유지하되 실제 호출 대상은 DeepSeek로 구현한다.
 *
 * 호출 대상: POST https://api.deepseek.com/v1/chat/completions (또는 동등 엔드포인트)
 * 헤더: Authorization: Bearer {api-key}, content-type: application/json
 * 실패(rate limit / 토큰 초과 / 5xx) → BusinessException(INTERNAL_ERROR).
 */
// TODO: @RequiredArgsConstructor + @Value 주입 (api-key, model)
@Component
public class AnthropicLlmClient implements LlmClient {

    // TODO: @Value("${llm.api-key}") String apiKey;
    // TODO: @Value("${llm.model:deepseek-chat}") String model;
    // TODO: final RestTemplate restTemplate; (또는 WebClient)

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        // TODO: complete 구현
        //       1) 요청 바디 구성: { model, max_tokens, messages:[{role:"user", content:...}], system:... }
        //       2) POST 호출 + 에러 처리
        //       3) 응답에서 content / usage(input_tokens, output_tokens) 추출
        //       4) LlmCompletionResponse로 매핑해 반환
        throw new UnsupportedOperationException("LLM 호출은 ai 도메인 PR에서 구현 (DeepSeek API)");
    }
}
