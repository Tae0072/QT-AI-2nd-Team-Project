package com.qtai.domain.qt.client.sum;

public record SuTodayPassage(
        String title,
        String koreanBookName,
        String englishBookName,
        Short chapter,
        Short startVerse,
        Short endVerse,
        String referenceText
) {
}
