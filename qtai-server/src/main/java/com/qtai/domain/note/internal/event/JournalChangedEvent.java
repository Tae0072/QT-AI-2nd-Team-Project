package com.qtai.domain.note.internal.event;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.internal.JournalEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record JournalChangedEvent(
        String eventId,
        JournalEventType eventType,
        Long memberId,
        Long noteId,
        Long qtPassageId,
        NoteCategory category,
        NoteStatus previousStatus,
        NoteStatus nextStatus,
        LocalDate savedDate,
        LocalDateTime occurredAt
) {
    public static JournalChangedEvent create(JournalEventType eventType, Long memberId, Long noteId,
                                             Long qtPassageId, NoteCategory category,
                                             NoteStatus previousStatus, NoteStatus nextStatus,
                                             LocalDate savedDate, LocalDateTime occurredAt) {
        return new JournalChangedEvent(
                UUID.randomUUID().toString(),
                eventType,
                memberId,
                noteId,
                qtPassageId,
                category,
                previousStatus,
                nextStatus,
                savedDate,
                occurredAt
        );
    }
}
