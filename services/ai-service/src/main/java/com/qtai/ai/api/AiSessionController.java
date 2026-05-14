package com.qtai.ai.api;

import com.qtai.ai.application.AiSessionService;
import com.qtai.ai.domain.AiSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

/**
 * AI 세션·턴 컨트롤러.
 *
 * <p>경로 (DECISIONS.md §3, AGENTS.md SSE 계약):
 * - POST /ai/sessions                  — 세션 시작 (오늘 QT 일치 검증, 불일치 시 422 AI_PASSAGE_NOT_TODAY_QT)
 * - GET  /ai/sessions                  — 본인 세션 목록
 * - GET  /ai/sessions/{id}             — 세션 + 턴 상세
 * - POST /ai/sessions/{id}/turns       — SSE 스트리밍 (turns 정식, /messages 금지)
 * - POST /ai/sessions/{id}/complete    — 완료 + ai.session.completed Kafka 발행
 *
 * <p>SSE 이벤트 (AGENTS.md): turn_started, token, sources(구 rag_sources), turn_completed, error, end.
 */
@RestController
@RequestMapping("/ai/sessions")
public class AiSessionController {

    private final AiSessionService service;

    public AiSessionController(AiSessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AiSession> create(@AuthenticationPrincipal Jwt jwt,
                                            @RequestBody StartRequest body) {
        Long userId = Long.valueOf(jwt.getSubject());
        AiSession session = service.startSession(userId, body);
        return ResponseEntity
                .created(URI.create("/ai/sessions/" + session.getId()))
                .body(session);
    }

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = Long.valueOf(jwt.getSubject());
        return service.listSessions(userId, status, page, size);
    }

    @GetMapping("/{sessionId}")
    public Map<String, Object> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sessionId) {
        return service.getSessionDetail(jwt, sessionId);
    }

    /**
     * SSE 스트리밍. 응답 Content-Type: text/event-stream.
     * 첫 토큰 timeout 5s / idle 30s / max 5분 — SseEmitter timeout으로 강제.
     * 클라이언트 끊김 시 emitter.onTimeout / onCompletion / onError 에서 DeepSeek Flux dispose.
     */
    @PostMapping(value = "/{sessionId}/turns", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter turns(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable Long sessionId,
                            @RequestBody TurnRequest body) {
        return service.streamTurn(jwt, sessionId, body.userMessage());
    }

    @PostMapping("/{sessionId}/complete")
    public AiSession complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long sessionId) {
        return service.completeSession(jwt, sessionId);
    }

    public record StartRequest(LocalDate qtDate, Passage passage, String guideStep) {}
    public record Passage(String bookCode, Integer chapter, Integer verse) {}
    public record TurnRequest(String userMessage) {}
}
