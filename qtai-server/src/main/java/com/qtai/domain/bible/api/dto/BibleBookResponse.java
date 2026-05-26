package com.qtai.domain.bible.api.dto;

public record BibleBookResponse(
        Short id,
        String testament,
        String code,
        String koreanName,
        String englishName,
        Short displayOrder
) {
}
