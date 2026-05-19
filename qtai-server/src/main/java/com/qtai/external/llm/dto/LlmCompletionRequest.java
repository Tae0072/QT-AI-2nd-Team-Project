package com.qtai.external.llm.dto;

/**
 * TODO: LLM completion 요청 DTO — model/prompt/maxTokens 등.
 */
public record LlmCompletionRequest(String model, String prompt, Integer maxTokens) {}
