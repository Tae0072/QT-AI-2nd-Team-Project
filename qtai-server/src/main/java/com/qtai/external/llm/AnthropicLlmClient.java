package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;
import org.springframework.stereotype.Component;

/**
 * TODO: 벤더명 기반 구현 (DeepSeek 등으로 교체 시 클래스명만 바꿔 식별성 유지).
 * v3.1 게이트: qtai-server에서 Anthropic SDK 직접 사용 금지 — DeepSeek API만 허용.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        throw new UnsupportedOperationException("LLM 호출은 ai 도메인 PR에서 구현 (DeepSeek API)");
    }
}
