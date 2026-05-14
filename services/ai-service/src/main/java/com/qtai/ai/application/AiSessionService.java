package com.qtai.ai.application;

import com.qtai.ai.api.AiSessionController;
import com.qtai.ai.domain.AiSession;
import com.qtai.ai.domain.AiTurn;
import com.qtai.ai.infrastructure.llm.DeepSeekStreamService;
import com.qtai.ai.infrastructure.persistence.AiSessionRepository;
import com.qtai.ai.infrastructure.persistence.AiTurnRepository;
import com.qtai.ai.prompt.QtPromptTemplates;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * AI 세션 UseCase.
 *
 * <p>TODO(강상민):
 * - startSession: 오늘 QT (BFF Aggregator의 /api/v1/qt/today 또는 Bible 직접) 일치 검증.
 *   불일치 시 IllegalArgumentException으로 AI_PASSAGE_NOT_TODAY_QT 422 매핑.
 * - streamTurn: Bible Service /api/v1/explanations/commentary/{...}로 sources 컨텍스트 적재 →
 *   DeepSeek 호출 → SseEmitter로 토큰 push → 마지막에 ai_turns INSERT + turn_completed/end.
 * - completeSession: 트랜잭션 안에서 status=COMPLETED + summary 저장,
 *   @TransactionalEventListener(AFTER_COMMIT)으로 Kafka 발행.
 */
@Service
public class AiSessionService {

    private final AiSessionRepository sessions;
    private final AiTurnRepository turns;
    private final DeepSeekStreamService deepSeek;
    private final QtPromptTemplates prompts;
    private final ApplicationEventPublisher eventPublisher;

    public AiSessionService(AiSessionRepository sessions,
                            AiTurnRepository turns,
                            DeepSeekStreamService deepSeek,
                            QtPromptTemplates prompts,
                            ApplicationEventPublisher eventPublisher) {
        this.sessions = sessions;
        this.turns = turns;
        this.deepSeek = deepSeek;
        this.prompts = prompts;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AiSession startSession(Long userId, AiSessionController.StartRequest body) {
        // TODO: 오늘 QT 일치 검증. 불일치 시 throw new IllegalArgumentException("AI_PASSAGE_NOT_TODAY_QT")
        AiSession s = new AiSession();
        s.setUserId(userId);
        s.setQtDate(body.qtDate());
        s.setBookCode(body.passage().bookCode());
        s.setChapter(body.passage().chapter());
        s.setVerseStart(body.passage().verse());
        s.setVerseEnd(body.passage().verse());
        s.setGuideStep(body.guideStep());
        s.setStatus("IN_PROGRESS");
        return sessions.save(s);
    }

    public Map<String, Object> listSessions(Long userId, String status, int page, int size) {
        var result = sessions.findAllByUserIdAndStatusOrderByCreatedAtDesc(
                userId, status == null ? "IN_PROGRESS" : status, PageRequest.of(page, size));
        return Map.of(
                "items", result.getContent(),
                "page", page,
                "size", size,
                "totalElements", result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetail(Jwt jwt, Long sessionId) {
        AiSession s = sessions.findById(sessionId).orElseThrow(() -> new NoSuchElementException("session not found"));
        // TODO: 소유자 검증 (s.userId == jwt.sub)
        List<AiTurn> turnList = turns.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        return Map.of("session", s, "turns", turnList);
    }

    public SseEmitter streamTurn(Jwt jwt, Long sessionId, String userMessage) {
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(5));
        AiSession s = sessions.findById(sessionId).orElseThrow(() -> new NoSuchElementException("session not found"));

        String systemPrompt = prompts.systemPrompt(s.getGuideStep());
        String userPrompt = prompts.userPromptWithContext(
                s.getBookCode() + " " + s.getChapter() + ":" + s.getVerseStart(),
                "(KR TODO: Bible Service에서 가져오기)",
                "(EN TODO: Bible Service에서 가져오기)",
                "(EXPL TODO: bible_explanations 컨텍스트)",
                userMessage);

        // turn_started
        sendSse(emitter, "turn_started", Map.of("sessionId", sessionId));

        deepSeek.stream(systemPrompt, userPrompt)
                .doOnNext(chunk -> sendSse(emitter, "token", Map.of("delta", chunk)))
                .doOnError(err -> {
                    sendSse(emitter, "error", Map.of("message", err.getMessage()));
                    emitter.completeWithError(err);
                })
                .doOnComplete(() -> {
                    // TODO: ai_turns INSERT (sources JSON 포함)
                    sendSse(emitter, "turn_completed", Map.of("sessionId", sessionId));
                    sendSse(emitter, "end", "[DONE]");
                    emitter.complete();
                })
                .subscribe();

        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    @Transactional
    public AiSession completeSession(Jwt jwt, Long sessionId) {
        AiSession s = sessions.findById(sessionId).orElseThrow(() -> new NoSuchElementException("session not found"));
        s.setStatus("COMPLETED");
        s.setSummary("TODO: AI 요약 생성 (last assistant turn 요약 또는 별도 LLM call)");
        sessions.save(s);
        eventPublisher.publishEvent(new AiSessionCompletedEvent(s));
        return s;
    }

    private void sendSse(SseEmitter emitter, String event, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(event).data(payload));
        } catch (IOException ignored) {
            // 클라이언트 끊김
        }
    }
}
