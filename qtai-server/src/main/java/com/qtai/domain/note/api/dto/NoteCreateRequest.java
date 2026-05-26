package com.qtai.domain.note.api.dto;

/** 노트 작성 요청 DTO. */
public record NoteCreateRequest(
        // TODO: String content   — 노트 본문 (필수, @NotBlank, 길이 제한 권장)
) {}
