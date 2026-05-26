package com.qtai.domain.ai.internal;

/**
 * AI 호출 감사용 로그 엔티티.
 *
 * 누가 언제 어떤 모델을 호출해 토큰을 얼마나 썼는지만 기록한다.
 * F-15 정책: 응답 본문(content)은 저장하지 않음 — 개인정보·민감 묵상 내용 보호.
 */
// TODO: @Entity, @Table(name = "ai_call_log")
public class AiCallLog {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long memberId;       — 호출자
    // TODO: String model;        — 사용된 모델 (예: deepseek-chat)
    // TODO: String promptType;   — REFLECTION / APPLICATION 등
    // TODO: Integer inputTokens;
    // TODO: Integer outputTokens;
    // TODO: LocalDateTime calledAt; — @CreationTimestamp
    // ※ content/response 본문은 의도적으로 저장하지 않음
}
