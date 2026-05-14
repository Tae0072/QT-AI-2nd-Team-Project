package com.qtai.bible.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ai.session.completed 컨슈머.
 *
 * <p>역할 (DECISIONS.md §4, AGENTS.md): 새 Journal을 만들지 않는다.
 * 오늘 QT Journal에 aiSessionId와 AI summary를 첨부할 뿐이다.
 *
 * <p>금지 패턴:
 * - idempotencyKey 검증 누락 (DataIntegrityViolationException catch + skip)
 * - 새 Journal 생성 (오늘 Journal에 attach만)
 *
 * <p>TODO(이지윤·이승욱):
 * - INBOX_KEYS 테이블에 idempotencyKey 적재 + UNIQUE 위반 시 skip
 * - 같은 (userId, qtDate)의 Journal 조회 → ai_session_id, ai_summary update
 * - journal.ai.attached 이벤트 append
 */
@Component
public class AiSessionCompletedConsumer {

    @KafkaListener(topics = "ai.session.completed", groupId = "bible-service.ai-session-completed")
    public void onMessage(String envelopeJson) {
        // TODO: ObjectMapper로 envelope 파싱, idempotencyKey 체크, journal에 attach
    }
}
