package com.qtai.bible.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Journal 이벤트 발행기.
 *
 * <p>⚠️ 반드시 @TransactionalEventListener(AFTER_COMMIT)로 호출. @Transactional 블록 내 직접 send 금지.
 *
 * <p>Envelope (DECISIONS.md §4):
 * eventId(ULID), eventType, eventVersion=1, schemaSubject, occurredAt, traceId,
 * producerService="bible-service", idempotencyKey, data{...}
 *
 * <p>TODO(이지윤·이승욱):
 * - ULID 생성, traceId/spanId 주입, ObjectMapper로 envelope 직렬화
 * - idempotencyKey 형식: "journal.created:{journalId}", "journal.update:{ULID}", "journal.delete:{journalId}:{epochMs}"
 */
@Component
public class JournalEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public JournalEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, String envelopeJson) {
        kafkaTemplate.send(topic, key, envelopeJson);
    }
}
