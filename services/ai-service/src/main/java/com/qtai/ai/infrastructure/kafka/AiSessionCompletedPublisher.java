package com.qtai.ai.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.ai.application.AiSessionCompletedEvent;
import com.qtai.ai.domain.AiSession;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;

/**
 * ai.session.completed Kafka 이벤트 발행기.
 *
 * <p>Envelope (DECISIONS.md §4): eventId(ULID), eventType, eventVersion=1,
 * schemaSubject="ai.session.completed-value", occurredAt, traceId,
 * producerService="ai-service", idempotencyKey="ai.session.completed:{sessionId}", data{...}
 *
 * <p>@TransactionalEventListener(AFTER_COMMIT) — 트랜잭션 커밋 완료 후 발행.
 * payload 키 사용 금지, data 키 사용. ObjectMapper로 JSON 직렬화.
 *
 * <p>TODO(강상민): ULID 생성, traceId/spanId OpenTelemetry 컨텍스트 주입.
 */
@Component
public class AiSessionCompletedPublisher {

    private static final String TOPIC = "ai.session.completed";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AiSessionCompletedPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                        ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCompleted(AiSessionCompletedEvent event) {
        publish(event.session());
    }

    private void publish(AiSession session) {
        Map<String, Object> envelope = Map.of(
                "eventId", "evt_TODO_ULID",
                "eventType", TOPIC,
                "eventVersion", 1,
                "schemaSubject", "ai.session.completed-value",
                "occurredAt", Instant.now().toString(),
                "traceId", "TODO",
                "producerService", "ai-service",
                "idempotencyKey", "ai.session.completed:" + session.getId(),
                "data", Map.of(
                        "sessionId", session.getId(),
                        "userId", session.getUserId(),
                        "qtDate", session.getQtDate().toString(),
                        "passage", Map.of(
                                "bookCode", session.getBookCode(),
                                "chapter", session.getChapter(),
                                "verseStart", session.getVerseStart(),
                                "verseEnd", session.getVerseEnd()
                        ),
                        "summary", session.getSummary() == null ? "" : session.getSummary()
                )
        );
        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, String.valueOf(session.getId()), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kafka envelope 직렬화 실패: sessionId=" + session.getId(), e);
        }
    }
}
