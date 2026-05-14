package com.qtai.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QT-AI AI Service.
 *
 * <p>역할: DeepSeek API (OpenAI 호환) 호출, 출처 기반 1회성 Q&A SSE 스트리밍,
 *         오늘 QT 세션 검증, ai.session.completed Kafka 이벤트 발행.
 *
 * <p>Owner: 강상민 (주도) · 강태오 (Lead) · 김태혁 — DECISIONS.md §0 (2026-05-14 재배치).
 *
 * <p>금지 패턴 (AGENTS.md):
 * - Anthropic SDK / Claude 고정 코드 / ANTHROPIC_API_KEY 사용 금지
 * - SSE 경로에 /messages 사용 금지 — 정식 경로는 /ai/sessions/{id}/turns
 * - 오늘 QT가 아닌 본문에서 세션 생성 금지 — 불일치 시 AI_PASSAGE_NOT_TODAY_QT 422
 * - RAG/ChromaDB/벡터 DB 신규 사용 금지 (ADR-0013, 2026-05-14 제거)
 * - sources(구 rag_sources) 키 사용. payload 키 사용 금지 (data 키 사용).
 */
@SpringBootApplication
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
