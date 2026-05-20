package com.qtai.external.llm.dto;

/** LLM 호출 요청 DTO (provider 중립). */
public record LlmCompletionRequest(
        // TODO: String model       — 모델 식별자 (예: "deepseek-chat")
        // TODO: String prompt      — 사용자 입력 프롬프트
        // TODO: Integer maxTokens  — 응답 최대 토큰 (기본 1024 권장)
        // 필요 시 systemPrompt, temperature 등 추가
        String model,
        String prompt,
        Integer maxTokens
) {}
