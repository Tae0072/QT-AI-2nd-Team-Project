package com.qtai.ai.presentation;

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
 * AI session endpoints.
 *
 * Canonical endpoints:
 * POST /ai/sessions
 * POST /ai/sessions/{id}/turns
 * GET /ai/sessions/{id}
 * GET /ai/sessions
 */
@RestController
@RequestMapping("/ai")
public class AiSessionController {

    @PostMapping("/sessions")
    public Object startSession(@Valid @RequestBody StartSessionRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @PostMapping(value = "/sessions/{sessionId}/turns", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter createTurn(
        @PathVariable String sessionId,
        @Valid @RequestBody TurnRequest request
    ) {
        return new SseEmitter(60_000L);
    }

    @GetMapping("/sessions/{sessionId}")
    public Object getSession(@PathVariable String sessionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @GetMapping("/sessions")
    public Object listSessions() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public record StartSessionRequest(
        String bookCode,
        Integer chapter,
        Integer verse,
        String promptType
    ) {
    }

    public record TurnRequest(
        String userMessage
    ) {
    }
}
