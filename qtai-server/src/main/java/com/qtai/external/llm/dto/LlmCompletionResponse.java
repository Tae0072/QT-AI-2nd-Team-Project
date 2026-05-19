package com.qtai.external.llm.dto;

/**
 * TODO: LLM completion 응답 DTO — content/tokensUsed 등.
 */
public record LlmCompletionResponse(String content, Integer tokensUsed) {}
