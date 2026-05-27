package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;

import java.util.List;

public record UpdateNoteCommand(
        NoteCategory category,
        Long qtPassageId,
        String title,
        String body,
        String rememberSection,
        String interpretSection,
        String applySection,
        String praySection,
        List<Long> verseIds,
        NoteStatus status,
        NoteVisibility visibility
) {
}
