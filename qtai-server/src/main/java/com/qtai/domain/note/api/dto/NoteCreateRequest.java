package com.qtai.domain.note.api.dto;

import java.util.List;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 노트 작성 요청 DTO. */
public record NoteCreateRequest(
        @NotNull NoteCategory category,
        @Size(max = 200) String title,
        String body,
        List<@NotNull Long> verseIds,
        NoteStatus status
) {}
