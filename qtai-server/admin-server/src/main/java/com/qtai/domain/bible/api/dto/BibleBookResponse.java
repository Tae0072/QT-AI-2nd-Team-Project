package com.qtai.domain.bible.api.dto;

public record BibleBookResponse(
        Integer id,
        String testament,
        String code,
        String koreanName,
        String englishName,
        Integer displayOrder
) {
}
