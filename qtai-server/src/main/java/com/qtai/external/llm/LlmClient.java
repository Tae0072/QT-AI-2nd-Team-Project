package com.qtai.external.llm;

import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

public interface LlmClient {

    LlmCompletionResponse complete(LlmCompletionRequest request);
}
