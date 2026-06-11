package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

/**
 * LLM 호출 포트 (provider-agnostic).
 *
 * 도메인(ai)은 이 인터페이스만 의존 — Anthropic/OpenAI/DeepSeek 등 교체 시
 * 도메인 변경 없이 구현체만 바꿔 끼울 수 있도록.
 */
public interface LlmClient {

    // TODO: 프롬프트를 보내고 모델 응답을 받는 단일 진입점
    LlmCompletionResponse complete(LlmCompletionRequest request);
}
