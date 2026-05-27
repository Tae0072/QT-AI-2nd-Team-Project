package com.qtai.domain.note.api.dto;

public record MeditationCalendarSummary(
        long savedDays,
        long savedNoteCount,
        long meditationStreakDays
) {
}
