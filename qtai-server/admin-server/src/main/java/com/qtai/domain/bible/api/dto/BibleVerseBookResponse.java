package com.qtai.domain.bible.api.dto;

public record BibleVerseBookResponse(
        String code,
        String koreanName,
        String englishName,
        Integer chapter
) {
}
