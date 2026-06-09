package com.qtai.domain.note.api.dto;

public record NoteVerseItem(
        Long bibleVerseId,
        String bookCode,
        Integer chapterNo,
        Integer verseNo,
        Integer displayOrder
) {
}
