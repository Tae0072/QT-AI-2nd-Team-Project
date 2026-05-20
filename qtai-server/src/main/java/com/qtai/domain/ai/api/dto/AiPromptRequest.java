package com.qtai.domain.ai.api.dto;

/** AI 프롬프트 요청 DTO. */
public record AiPromptRequest(
        // TODO: Long qtId          — 컨텍스트로 사용할 QT ID (선택, null 가능)
        // TODO: String prompt      — 사용자 추가 질문/지시 (필수, @NotBlank)
        // TODO: String promptType  — REFLECTION / APPLICATION / PRAYER 등 사전 정의 타입
) {}
