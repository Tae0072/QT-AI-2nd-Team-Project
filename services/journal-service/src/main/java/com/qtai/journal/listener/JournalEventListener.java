package com.qtai.journal.listener;

import com.qtai.journal.event.JournalCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleJournalCreated(JournalCreatedEvent event) {
        // Kafka envelope 표준: data 키 사용
        Map<String, Object> envelope = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", "journal.created",
            "data", Map.of(
                "journalId", event.journalId(),
                "userId", event.userId()
            )
        );
        log.info("Publishing journal.created event: journalId={}", event.journalId());
        kafkaTemplate.send("journal.created", envelope);
    }
}
