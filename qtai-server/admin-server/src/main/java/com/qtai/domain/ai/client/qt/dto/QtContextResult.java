package com.qtai.domain.ai.client.qt.dto;

public record QtContextResult(
        Long passageId,
        String bibleBook,
        Integer chapter,
        Integer startVerse,
        Integer endVerse,
        String passageReference,
        String title,
        String summary,
        String passageContext
) {
}
