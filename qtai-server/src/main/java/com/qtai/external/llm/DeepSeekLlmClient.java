package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;
import org.springframework.stereotype.Component;

/**
 * DeepSeek OpenAI-compatible API 호출 구현체.
 *
 * CLAUDE.md §1: AI는 DeepSeek OpenAI-compatible client 만 허용.
 * Anthropic SDK 직접 사용 금지.
 *
 * 호출 대상: POST https://api.deepseek.com/v1/chat/completions
 * 헤더: Authorization: Bearer {api-key}, Content-Type: application/json
 * 실패(rate limit / 토큰 초과 / 5xx) → BusinessException(INTERNAL_ERROR).
 * API key 는 로그에 절대 남기지 않는다 (CLAUDE.md §7).
 */
// TODO: @RequiredArgsConstructor + @Value 주입 (api-key, model)
@Component
public class DeepSeekLlmClient implements LlmClient {

    // TODO: @Value("${external.llm.api-key}") String apiKey;
    // TODO: @Value("${external.llm.model:deepseek-chat}") String model;
    // TODO: final RestTemplate restTemplate; (또는 WebClient)

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        // TODO: complete 구현
        //       1) 요청 바디 구성: { model, max_tokens, messages:[{role:"user", content:...}], system:... }
        //       2) POST 호출 + 에러 처리
        //       3) 응답에서 content / usage(input_tokens, output_tokens) 추출
        //       4) LlmCompletionResponse 로 매핑해 반환
        throw new UnsupportedOperationException("LLM 호출은 ai 도메인 PR에서 구현 (DeepSeek API)");
    }
}
