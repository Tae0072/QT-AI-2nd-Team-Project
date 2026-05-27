package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;

import java.time.LocalDate;
import java.util.List;

public record MeditationCalendarDay(
        LocalDate date,
        boolean saved,
        long savedNoteCount,
        Long meditationNoteId,
        List<NoteCategory> categories
) {
}
