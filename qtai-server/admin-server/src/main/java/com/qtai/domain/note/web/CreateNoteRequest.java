package com.qtai.domain.note.web;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import com.qtai.domain.note.api.dto.CreateNoteCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateNoteRequest(
        @NotNull
        NoteCategory category,
        Long qtPassageId,
        @Size(max = 200)
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

    CreateNoteCommand toCommand() {
        return new CreateNoteCommand(
                category,
                qtPassageId,
                title,
                body,
                rememberSection,
                interpretSection,
                applySection,
                praySection,
                verseIds,
                status,
                visibility
        );
    }
}
