package com.qtai.domain.bible.api.dto;

public record BibleVerseResponse(
        Long id,
        String bookCode,
        Integer chapterNo,
        Integer verseNo,
        String koreanText,
        String englishText
) {
}
