package com.qtai.ai.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for ai.session.completed.
 */
@Component
public class AiSessionCompletedPublisher {

    private static final String TOPIC = "ai.session.completed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:ai-service}")
    private String producerService;

    public AiSessionCompletedPublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

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
        envelope.put("data", data);

        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, sessionId, json);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish " + TOPIC + " event", ex);
        }
    }
}
