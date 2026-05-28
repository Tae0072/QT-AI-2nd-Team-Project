package com.qtai.domain.note.api.dto;

import com.qtai.domain.note.api.NoteCategory;

import java.time.LocalDate;
import java.util.List;

public record MeditationCalendarResponse(
        String month,
        List<Day> days,
        Summary summary
) {

    public record Day(
            LocalDate date,
            boolean saved,
            long savedNoteCount,
            Long meditationNoteId,
            List<NoteCategory> categories
    ) {
    }

    public record Summary(
            long savedDays,
            long savedNoteCount,
            long meditationStreakDays
    ) {
    }
}
