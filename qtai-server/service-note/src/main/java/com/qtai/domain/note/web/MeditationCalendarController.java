package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequiredArgsConstructor
public class MeditationCalendarController {

    private final GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    private final Clock clock;

    @GetMapping("/api/v1/me/meditation-calendar")
    public ApiResponse<MeditationCalendarResponse> getCalendar(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String month) {
        Long authenticatedMemberId = requireMemberId(memberId);
        YearMonth targetMonth = parseMonth(month);
        return ApiResponse.success(getMeditationCalendarUseCase.getCalendar(authenticatedMemberId, targetMonth));
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(clock);
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month는 yyyy-MM 형식이어야 합니다.");
        }
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
