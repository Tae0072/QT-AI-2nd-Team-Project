package com.qtai.domain.note.internal;

import com.qtai.common.entity.BaseEntity;
import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_journal_events_event_id", columnNames = "event_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "qt_passage_id")
    private Long qtPassageId;

    /** 변경 전 QT 본문 id — 본문이 바뀐 변경에서 증분 집계(이전 -1, 새 +1)를 위한 선반영 컬럼(P1-10). */
    @Column(name = "previous_qt_passage_id")
    private Long previousQtPassageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private JournalEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 10)
    private NoteStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 10)
    private NoteStatus currentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalEventStatus status;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    /** 재처리기 백오프 재시도 예약 시각 — NULL이면 즉시 대상(주로 PENDING)(P1-10). */
    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    private JournalEvent(JournalChangedEvent event) {
        this.eventId = event.eventId();
        this.memberId = event.memberId();
        this.noteId = event.noteId();
        this.qtPassageId = event.qtPassageId();
        this.previousQtPassageId = event.previousQtPassageId();
        this.eventType = event.eventType();
        this.previousStatus = event.previousStatus();
        this.currentStatus = event.currentStatus();
        this.status = JournalEventStatus.PENDING;
        this.occurredAt = event.occurredAt();
        this.retryCount = 0;
    }

    public static JournalEvent pending(JournalChangedEvent event) {
        return new JournalEvent(event);
    }

    public void markProcessed(LocalDateTime processedAt) {
        this.status = JournalEventStatus.PROCESSED;
        this.processedAt = processedAt;
        this.failedAt = null;
        this.nextAttemptAt = null;
        this.lastErrorMessage = null;
    }

    /**
     * 처리 실패를 기록하고 다음 재시도 시각을 예약한다(P1-10 재처리기용).
     *
     * <p>상태는 FAILED로 두고 {@code lastErrorMessage}/{@code failedAt}/{@code retryCount}를 남겨
     * 운영 가시성과 재처리 가능 상태를 보존한다. {@code nextAttemptAt}이 도래하고 재시도 상한 미만이면
     * 재처리기가 다시 집어 든다.
     *
     * @param errorMessage  실패 원인(최대 500자 절단)
     * @param failedAt      실패 기록 시각
     * @param nextAttemptAt 다음 재시도 예약 시각(백오프). NULL이면 더는 자동 재시도하지 않는 사실상 dead-letter
     */
    public void markFailedForRetry(String errorMessage, LocalDateTime failedAt, LocalDateTime nextAttemptAt) {
        this.status = JournalEventStatus.FAILED;
        this.failedAt = failedAt;
        this.nextAttemptAt = nextAttemptAt;
        this.lastErrorMessage = truncate(errorMessage);
        this.retryCount++;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
