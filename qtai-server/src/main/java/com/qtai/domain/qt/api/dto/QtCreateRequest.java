package com.qtai.domain.qt.api.dto;

/** QT 작성 요청 DTO. */
public record QtCreateRequest(
        // TODO: String content              — 묵상 본문 (필수, @NotBlank)
        // TODO: Long bibleVerseId           — 참조 성경 절 ID (선택, null 가능)
        // TODO: QtVisibility visibility     — PRIVATE / PUBLIC (기본 PRIVATE)
        // TODO: String title                — 제목 (선택)
) {}
