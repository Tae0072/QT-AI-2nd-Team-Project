package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;
import org.springframework.stereotype.Component;

@Component
public class AnthropicLlmClient implements LlmClient {

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        throw new UnsupportedOperationException("Anthropic 호출은 ai 도메인 PR에서 구현");
    }
}
