package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteStatus;

public record NoteSaveResponse(
        Long id,
        NoteStatus status
) {
}
