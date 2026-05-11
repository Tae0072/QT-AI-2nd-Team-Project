package com.qtai.ai.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI Session Controller
 *
 * 엔드포인트 (DECISIONS.md §3 기준):
 *   POST   /ai/sessions                  — 세션 시작
 *   POST   /ai/sessions/{id}/turns       — 대화 (SSE 스트리밍) ← /messages 금지!
 *   GET    /ai/sessions/{id}             — 세션 조회
 *   GET    /ai/sessions                  — 세션 목록
 *
 * SSE 이벤트 계약:
 *   turn_started → token (반복) → rag_sources → turn_completed → [DONE]
 */
@RestController
@RequestMapping("/ai")
public class AiSessionController {

    /**
     * AI 세션 시작 — ChromaDB RAG 검색 + 세션 생성
     */
    @PostMapping("/sessions")
    public Object startSession(@Valid @RequestBody StartSessionRequest req) {
        // TODO: ChromaDB 검색 + 세션 생성 + DB 저장
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * AI 대화 — SSE 스트리밍
     *
     * 구현 개요:
     *   1. SseEmitter 생성 (timeout 60s)
     *   2. 비동기 스레드에서 Claude API createStreaming() 호출
     *   3. 각 token 이벤트 도착 시 emitter.send(SseEmitter.event().name("token").data(t))
     *   4. 완료 시 emitter.send("turn_completed") → emitter.send("[DONE]") → emitter.complete()
     *   5. 세션 끝나면 ai.session.completed Kafka 이벤트 발행
     */
    @PostMapping(value = "/sessions/{sessionId}/turns",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createTurn(@PathVariable String sessionId,
                                  @Valid @RequestBody TurnRequest req) {
        SseEmitter emitter = new SseEmitter(60_000L);
        // TODO: 비동기 실행 → Claude 스트리밍 → SseEmitter로 릴레이
        return emitter;
    }

    /**
     * 세션 조회
     */
    @GetMapping("/sessions/{sessionId}")
    public Object getSession(@PathVariable String sessionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * 세션 목록
     */
    @GetMapping("/sessions")
    public Object listSessions() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // === Request DTOs ===

    public record StartSessionRequest(
        String bookCode,
        Integer chapter,
        Integer verse,
        String promptType   // A, B, C, D
    ) {}

    public record TurnRequest(
        String userMessage
    ) {}
}
