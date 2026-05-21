package com.qtai.domain.note.internal;

/**
 * 묵상 노트 이벤트 이력 엔티티.
 *
 * MEDITATION 카테고리 노트가 SAVED 상태로 전환될 때
 * ApplicationEventPublisher 를 통해 이벤트를 발행하고,
 * 핸들러에서 이 테이블에 이력을 남긴다.
 *
 * 이벤트 핸들러 실패 시 Kafka 없이 로그만 기록한다 (CLAUDE.md §8 금지 기술).
 * 로그에는 eventId, event type, handler name, error message 를 포함한다 (CLAUDE.md §9).
 *
 * DDL 예시:
 *   CREATE TABLE journal_events (
 *       id         BIGINT AUTO_INCREMENT PRIMARY KEY,
 *       note_id    BIGINT NOT NULL,
 *       event_type VARCHAR(50) NOT NULL,   -- 예: NOTE_SAVED, NOTE_DELETED
 *       occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *       FOREIGN KEY (note_id) REFERENCES notes(id)
 *   );
 */
// TODO: @Entity, @Table(name = "journal_events")
public class JournalEvent {

    // TODO: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

    // TODO: @ManyToOne(fetch = FetchType.LAZY)
    //        @JoinColumn(name = "note_id", nullable = false)
    //        Note note;

    // TODO: @Column(nullable = false, length = 50)
    //        String eventType;    — 예: "NOTE_SAVED", "NOTE_DELETED"

    // TODO: @Column(nullable = false)
    //        LocalDateTime occurredAt;   — @CreationTimestamp
}
