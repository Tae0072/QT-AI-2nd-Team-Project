package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;

public record NoteCategoryItem(
        NoteCategory category,
        String label,
        boolean requiresQtPassage,
        boolean supportsVerseSelection,
        boolean writableFromList
) {
}
