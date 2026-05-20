package com.qtai.external.llm.dto;

/** LLM 응답 DTO. */
public record LlmCompletionResponse(
        // TODO: String content      — 생성된 응답 텍스트
        // TODO: Integer tokensUsed  — 사용된 총 토큰 수 (요금/사용량 추적용)
        // 필요 시 inputTokens/outputTokens 분리 또는 model 식별자 추가
        String content,
        Integer tokensUsed
) {}
