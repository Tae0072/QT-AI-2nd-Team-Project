package com.qtai.bible.journal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Journal 이벤트 소싱 스토어 (truth). 02_ERD §5.3.
 *
 * <p>⚠️ append-only. 수정/삭제 코드 작성 금지.
 * journal_id별 sequence는 SELECT MAX(sequence) FROM journal_events WHERE journal_id=? FOR UPDATE 로 부여.
 */
@Entity
@Table(
        name = "journal_events",
        uniqueConstraints = @UniqueConstraint(name = "uq_journal_seq", columnNames = {"journal_id", "sequence"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_id", nullable = false)
    private Long journalId;

    @Column(nullable = false)
    private Long sequence;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;   // journal.created, journal.updated, journal.deleted, ai.summary.attached

    @Column(name = "event_data", columnDefinition = "JSON", nullable = false)
    private String eventData;   // payload JSON 문자열

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
