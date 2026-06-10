package com.qtai.external.llm.dto;

/** LLM 호출 요청 DTO (provider 중립). */
public record LlmCompletionRequest(
        String model,
        String systemPrompt,
        String prompt,
        Integer maxTokens,
        Double temperature
) {
}
