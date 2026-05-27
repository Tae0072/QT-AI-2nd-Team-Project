package com.qtai.domain.note.api.dto;

import java.util.List;

public record MeditationCalendarResponse(
        String month,
        List<MeditationCalendarDay> days,
        MeditationCalendarSummary summary
) {
}
