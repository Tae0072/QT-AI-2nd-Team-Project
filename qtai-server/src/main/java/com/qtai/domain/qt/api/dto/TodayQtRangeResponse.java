package com.qtai.domain.qt.api.dto;

public record TodayQtRangeResponse(
        String testament,
        String bookCode,
        String koreanBookName,
        String englishBookName,
        Integer chapter,
        Integer verseFrom,
        Integer verseTo,
        String displayText
) {
}
