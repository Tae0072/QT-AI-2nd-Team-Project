package com.qtai.domain.note.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;

public record NoteListItem(
        Long id,
        NoteCategory category,
        String title,
        String bodyPreview,
        NoteStatus status,
        NoteVisibility visibility,
        LocalDate qtDate,
        String rangeLabel,
        boolean shared,
        LocalDateTime savedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
