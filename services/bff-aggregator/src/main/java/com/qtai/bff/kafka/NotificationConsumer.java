package com.qtai.bff.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * notification.requested 컨슈머.
 *
 * <p>journal.created, ai.session.completed 등에서 발행된 알림을
 * STOMP /user/{userId}/queue/notifications로 푸시한다.
 *
 * <p>TODO(강태오): envelope 파싱, idempotency 검증, SimpMessagingTemplate.convertAndSendToUser.
 */
@Component
public class NotificationConsumer {

    private final SimpMessagingTemplate template;

    public NotificationConsumer(SimpMessagingTemplate template) {
        this.template = template;
    }

    @KafkaListener(topics = "notification.requested", groupId = "bff-aggregator.notifications")
    public void onMessage(String envelopeJson) {
        // TODO: parse envelope.data.userId → template.convertAndSendToUser(userId, "/queue/notifications", payload)
    }
}
