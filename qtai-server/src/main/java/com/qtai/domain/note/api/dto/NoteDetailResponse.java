package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record NoteDetailResponse(
        Long id,
        Long memberId,
        NoteCategory category,
        Long qtPassageId,
        String title,
        String body,
        String rememberSection,
        String interpretSection,
        String applySection,
        String praySection,
        NoteStatus status,
        NoteVisibility visibility,
        LocalDate qtDate,
        String rangeLabel,
        boolean shared,
        LocalDateTime savedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<NoteVerseItem> verses
) {
}
