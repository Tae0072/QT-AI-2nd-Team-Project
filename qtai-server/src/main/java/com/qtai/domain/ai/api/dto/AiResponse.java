package com.qtai.domain.ai.api.dto;

/** AI 단발 응답 DTO. */
public record AiResponse(
        // TODO: String content      — 생성된 응답 텍스트
        // TODO: Integer tokensUsed  — 사용 토큰 수 (요금 추적)
        // TODO: String model        — 응답 모델 식별자
        // TODO: LocalDateTime generatedAt — 생성 시각
) {}
