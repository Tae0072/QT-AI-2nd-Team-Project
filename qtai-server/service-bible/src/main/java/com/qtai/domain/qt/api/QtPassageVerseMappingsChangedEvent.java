package com.qtai.domain.qt.api;

import java.util.UUID;

public record QtPassageVerseMappingsChangedEvent(
        String eventId,
        String eventType,
        Long qtPassageId
) {

    public static final String EVENT_TYPE = "QT_PASSAGE_VERSE_MAPPINGS_CHANGED";

    public QtPassageVerseMappingsChangedEvent(Long qtPassageId) {
        this(UUID.randomUUID().toString(), EVENT_TYPE, qtPassageId);
    }

    public QtPassageVerseMappingsChangedEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        if (eventType == null || eventType.isBlank()) {
            eventType = EVENT_TYPE;
        }
        if (qtPassageId == null) {
            throw new IllegalArgumentException("qtPassageId is required.");
        }
    }
}
