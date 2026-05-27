package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.MeditationCalendarResponse;

import java.time.YearMonth;

public interface GetMeditationCalendarUseCase {

    MeditationCalendarResponse get(Long memberId, YearMonth month);
}
