package com.qtai.domain.note.api.dto;

public record NoteDraftResponse(
        boolean exists,
        NoteDetailResponse note
) {
}
