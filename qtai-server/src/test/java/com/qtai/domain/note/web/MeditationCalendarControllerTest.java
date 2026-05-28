package com.qtai.domain.note.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeditationCalendarControllerTest {

    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    private MeditationCalendarController controller;

    @BeforeEach
    void setUp() {
        getMeditationCalendarUseCase = mock(GetMeditationCalendarUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        controller = new MeditationCalendarController(getMeditationCalendarUseCase, clock);
    }

    @Test
    @DisplayName("authenticated request delegates member and month")
    void getCalendar_delegatesMemberAndMonth() {
        MeditationCalendarResponse response = new MeditationCalendarResponse(
                "2026-05",
                List.of(new MeditationCalendarResponse.Day(LocalDate.of(2026, 5, 28), false, 0, null, List.of())),
                new MeditationCalendarResponse.Summary(0, 0, 0)
        );
        when(getMeditationCalendarUseCase.getCalendar(1L, YearMonth.of(2026, 5))).thenReturn(response);

        ApiResponse<MeditationCalendarResponse> result = controller.getCalendar(1L, "2026-05");

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isSameAs(response);
        verify(getMeditationCalendarUseCase).getCalendar(1L, YearMonth.of(2026, 5));
    }

    @Test
    @DisplayName("missing month uses current KST month")
    void getCalendar_missingMonth_usesCurrentKstMonth() {
        MeditationCalendarResponse response = new MeditationCalendarResponse(
                "2026-05", List.of(), new MeditationCalendarResponse.Summary(0, 0, 0));
        when(getMeditationCalendarUseCase.getCalendar(1L, YearMonth.of(2026, 5))).thenReturn(response);

        ApiResponse<MeditationCalendarResponse> result = controller.getCalendar(1L, null);

        assertThat(result.data().month()).isEqualTo("2026-05");
        verify(getMeditationCalendarUseCase).getCalendar(1L, YearMonth.of(2026, 5));
    }

    @Test
    @DisplayName("invalid month format is rejected")
    void getCalendar_invalidMonth_rejected() {
        assertThatThrownBy(() -> controller.getCalendar(1L, "2026-5"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("missing member id is unauthorized")
    void getCalendar_missingMember_rejected() {
        assertThatThrownBy(() -> controller.getCalendar(null, "2026-05"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
