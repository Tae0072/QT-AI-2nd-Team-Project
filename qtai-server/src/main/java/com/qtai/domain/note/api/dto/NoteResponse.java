package com.qtai.domain.note.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;

/** 노트 응답 DTO. */
public record NoteResponse(
        Long id,
        NoteCategory category,
        NoteStatus status,
        String visibility,
        String title,
        String body,
        List<Long> verseIds,
        LocalDateTime createdAt,
        LocalDateTime savedAt
) {}
