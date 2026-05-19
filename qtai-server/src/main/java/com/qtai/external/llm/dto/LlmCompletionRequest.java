package com.qtai.external.llm.dto;

public record LlmCompletionRequest(String model, String prompt, Integer maxTokens) {}
