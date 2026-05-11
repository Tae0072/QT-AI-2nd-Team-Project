package com.qtai.ai.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ai.session.completed 이벤트 발행기.
 *
 * Envelope 표준 (DECISIONS.md §4):
 *   eventId, eventType, eventVersion, schemaSubject,
 *   occurredAt, traceId, producerService, idempotencyKey,
 *   data   ← ⚠️ payload 아님, data
 *
 * 주의: 세션 완료 제어 흐름에서
 *   → @TransactionalEventListener(phase = AFTER_COMMIT) 패턴으로 발행할 것
 *   → Kafka 송신 실패가 DB 커밋에 영향 주지 않도록
 */
@Component
public class AiSessionCompletedPublisher {

    private static final String TOPIC = "ai.session.completed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:ai-service}")
    private String producerService;

    public AiSessionCompletedPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                        ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * ai.session.completed 이벤트 발행.
     *
     * @param sessionId AI 세션 ID (envelope 키로 사용)
     * @param data      이벤트 페이로드 — envelope.data 아래에 들어감
     * @param traceId   OpenTelemetry trace ID
     */
    public void publish(String sessionId, Map<String, Object> data, String traceId) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("eventId", "evt_" + UUID.randomUUID());
        envelope.put("eventType", TOPIC);
        envelope.put("eventVersion", 1);
        envelope.put("schemaSubject", TOPIC + "-value");
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("traceId", traceId);
        envelope.put("producerService", producerService);
        envelope.put("idempotencyKey", TOPIC + ":" + sessionId);
        envelope.put("data", data);   // ⚠️ payload 아님

        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, sessionId, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish " + TOPIC + " event", e);
        }
    }
}
