package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

/**
 * TODO: LLM 호출 — interface. ai 도메인에서 직접 주입.
 */
public interface LlmClient {

    LlmCompletionResponse complete(LlmCompletionRequest request);
}
