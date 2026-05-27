package com.qtai.domain.note.internal;

import com.qtai.common.entity.BaseEntity;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.internal.event.JournalChangedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_journal_events_event_id", columnNames = "event_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private JournalEventType eventType;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "qt_passage_id")
    private Long qtPassageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 10)
    private NoteStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_status", nullable = false, length = 10)
    private NoteStatus nextStatus;

    @Column(name = "saved_date")
    private LocalDate savedDate;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    private JournalEvent(JournalChangedEvent event) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.memberId = event.memberId();
        this.noteId = event.noteId();
        this.qtPassageId = event.qtPassageId();
        this.category = event.category();
        this.previousStatus = event.previousStatus();
        this.nextStatus = event.nextStatus();
        this.savedDate = event.savedDate();
        this.occurredAt = event.occurredAt();
    }

    public static JournalEvent from(JournalChangedEvent event) {
        return new JournalEvent(event);
    }
}
