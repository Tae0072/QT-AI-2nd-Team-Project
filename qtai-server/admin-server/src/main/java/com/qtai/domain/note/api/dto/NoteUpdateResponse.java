package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;

import java.time.LocalDateTime;

public record NoteUpdateResponse(
        Long id,
        NoteCategory category,
        NoteStatus status,
        NoteVisibility visibility,
        String activeUniqueKey,
        LocalDateTime savedAt,
        LocalDateTime updatedAt,
        Boolean sharingSnapshotUpdated
) {
}
