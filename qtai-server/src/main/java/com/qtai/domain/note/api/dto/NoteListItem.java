package com.qtai.domain.note.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.qtai.domain.note.internal.NoteCategory;
import com.qtai.domain.note.internal.NoteStatus;

public record NoteListItem(
        Long id,
        NoteCategory category,
        String title,
        NoteStatus status, String visibility,
        LocalDate qtDate,
        String rangeLabel,
        boolean shared,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

}
