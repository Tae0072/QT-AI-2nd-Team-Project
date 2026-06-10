package com.qtai.domain.note.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record JournalChangedEvent(
        UUID eventId,
        Long memberId,
        Long noteId,
        Long qtPassageId,
        Long previousQtPassageId,
        JournalEventType eventType,
        NoteStatus previousStatus,
        NoteStatus currentStatus,
        LocalDateTime occurredAt
) {
}
