package com.qtai.domain.ai.client.qt.dto;

public record QtContextResult(
        Long qtPassageId,
        String bibleBook,
        int chapter,
        int startVerse,
        int endVerse,
        String passageReference,
        String qtTitle,
        String promptContextSummary
) {
}
