package com.qtai.external.llm.dto;

/** LLM 응답 DTO. */
public record LlmCompletionResponse(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String model
) {
}
