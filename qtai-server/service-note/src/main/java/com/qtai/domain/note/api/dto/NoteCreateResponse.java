package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;

import java.time.LocalDateTime;

public record NoteCreateResponse(
        Long id,
        NoteCategory category,
        NoteStatus status,
        NoteVisibility visibility,
        Long sharedPostId,
        LocalDateTime createdAt
) {
}
